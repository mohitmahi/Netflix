package cache;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;
import model.LeaderBoardEntryItem;
import model.LeaderBoardOutputItem;
import org.springframework.stereotype.Component;
import utils.ApiPathUtil;
import utils.CustomViewsUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static git.GitClientManager.GITHUB_EVENT_ADDRESS;
import static utils.ApiPathUtil.PROXY_PATH_PAGINATED_REFRESH.PATH_ORG_NETFLIX_MEMBERS;
import static utils.ApiPathUtil.PROXY_PATH_PAGINATED_REFRESH.PATH_ORG_NETFLIX_REPOS;
import static utils.CustomViewsUtil.CUSTOM_VIEWS.last_updated;

/*
RedisClientManager is a worker verticle with its own dedicated worker thread pool, which will listen to event bus
 * only for REDIS_MAP_EVENT_ADDRESS and REDIS_RANK_EVENT_ADDRESS.
 * REDIS_MAP_EVENT_ADDRESS is for KEY based look up in redis.
 * REDIS_RANK_EVENT_ADDRESS is for ZRANGE based sorted set lookup in redis.
 * This worker verticle will also periodically refresh cache for both KEYS and SET lookup.
 * This worker verticle also publish event to git-client-manager if any git api query invocation required.
 */
@Component
public class RedisClientManager extends AbstractVerticle {
    private RedisAPI redisAPI;

    public static final String REDIS_MAP_EVENT_ADDRESS = "redisMap";
    public static final String REDIS_SET_EVENT_ADDRESS = "redisSet";
    public static final String REDIS_RANK_EVENT_ADDRESS = "redisRank";
    AtomicBoolean leaderBoardReady = new AtomicBoolean(Boolean.FALSE);

    @Override
    public void start() {
        vertx.eventBus().<String>consumer(REDIS_MAP_EVENT_ADDRESS).handler(handleRedisGETJsonRequest());
        vertx.eventBus().<String>consumer(REDIS_SET_EVENT_ADDRESS).handler(handleRedisGETArrayNodeRequest());
        vertx.eventBus().<String>consumer(REDIS_RANK_EVENT_ADDRESS).handler(handleRedisRankRequest());
        vertx.setPeriodic(5000, id -> handleRedisRefreshTask()); //refresh every 5 sec

        Redis client = Redis.createClient(vertx, new RedisOptions()
                .setMaxPoolSize(10)
                .setMaxWaitingHandlers(50));
        redisAPI = RedisAPI.api(client);
        redisAPI.flushall(List.of("SYNC"));
        handleRedisRefreshTask();
    }

    private void handleRedisRefreshTask() {
        System.out.println("Refresh Timer fired!");
        Arrays.stream(ApiPathUtil.PROXY_PATH_REFRESH.values()).map(path -> path.value).forEach(this::getAndSetInCacheAsKey);
        Arrays.stream(ApiPathUtil.PROXY_PATH_PAGINATED_REFRESH.values()).map(path -> path.value).forEach(pathName -> getAndSetInCacheAsSet(pathName, pathName));
    }

    private void updateBottomNRankedItem() {
        System.out.println("Updating Leaderboard");
        redisAPI
                .smembers(PATH_ORG_NETFLIX_REPOS.value)
                .onFailure(t -> System.out.println("Redis GET Response failed " + t))
                .onSuccess(message -> {
                    if (message == null) {
                        System.out.println("Missing in cache, getting leaderboard ready in a moment");
                    } else {
                        System.out.println("Redis smembers for Ranking");
                        final JsonArray array = new JsonArray(message.toString());
                        Arrays.stream(CustomViewsUtil.CUSTOM_VIEWS.values()).iterator().forEachRemaining(custom_views -> {
                            List<String> list = new ArrayList<>();
                            list.add(0, custom_views.setName);
                            for (int i = 0; i < array.size(); i++) {
                                JsonObject jsonObject = array.getJsonObject(i);
                                list.add(getLeaderBoardItem(jsonObject, custom_views).getScore());
                                list.add(getLeaderBoardItem(jsonObject, custom_views).getKey());
                            }
                            System.out.println("Adding to LB:: " + list);
                            redisAPI.zadd(list)
                                    .onFailure(t -> {
                                        System.out.println("Redis refresh zadd Response failed " + t);
                                    });
                        });
                    }
                    leaderBoardReady.set(Boolean.TRUE);
                });
    }

    private LeaderBoardEntryItem getLeaderBoardItem(JsonObject jsonObject, CustomViewsUtil.CUSTOM_VIEWS custom_views) {
        String score;
        if (custom_views.viewName.equals(last_updated.viewName)) {
            final String time = jsonObject.getString(custom_views.viewName);
            Instant timestamp = Instant.parse(time);
            long timeInMillis = timestamp.toEpochMilli();
            score = String.valueOf(timeInMillis);
        } else {
            score = jsonObject.getString(custom_views.viewName);
        }
        final String repoName = jsonObject.getString("full_name");
        LeaderBoardEntryItem item = LeaderBoardEntryItem.builder().key(repoName).setName(custom_views.setName).score(score).build();
        return item;
    }

    private Handler<Message<String>> handleRedisGETJsonRequest() {
        return msg -> {
            String pathName = msg.body();
            redisAPI
                    .get(pathName)
                    .onFailure(t -> System.out.println("Redis get Response failed " + t))
                    .onSuccess(message -> {
                        if (message == null) {
                            System.out.println("Missing in redis, updating cache entry");
                            getAndSetInCacheAsKey(pathName, msg);
                        } else {
                            System.out.println("Redis API response1" + message);
                            msg.reply(message.toString());
                        }
                    });
        };
    }

    private Handler<Message<String>> handleRedisGETArrayNodeRequest() {
        return msg -> {
            String pathName = msg.body();
            final String setKey;
            if (pathName.contains("repos?page") || pathName.contains("members?page")) {
                setKey = pathName.contains("repos?page") ? PATH_ORG_NETFLIX_REPOS.value : PATH_ORG_NETFLIX_MEMBERS.value;
                getAndSetInCacheAsSet(pathName, setKey, msg);
            } else {
                redisAPI
                        .smembers(pathName)
                        .onFailure(t -> System.out.println("Redis smembers Response failed " + t))
                        .onSuccess(message -> {
                            if (message == null) {
                                System.out.println("Missing in redis, updating cache entry");
                                getAndSetInCacheAsSet(pathName, pathName, msg);
                            } else {
                                System.out.println("Redis SMembers Successful: " + pathName + " with key " + pathName);
                                msg.reply(message.toString());
                            }
                        });
            }
        };
    }

    private void getAndSetInCacheAsKey(String pathName) {
        getAndSetInCacheAsKey(pathName, null);
    }

    private void getAndSetInCacheAsSet(String pathName, String setKey) {
        getAndSetInCacheAsSet(pathName, setKey, null);
    }

    private void getAndSetInCacheAsSet(String pathName, String setKey,Message<String> message) {
        vertx.eventBus().request(GITHUB_EVENT_ADDRESS, pathName, response -> {
            if (response.succeeded()) {
                final String value = response.result().body().toString();
                final JsonArray array = new JsonArray(response.result().body().toString());
                ArrayList<String> paramList = new ArrayList<>(array.size() + 1);
                paramList.add(0, setKey);
                for(int i = 0; i < array.size(); i++){
                    paramList.add(array.getJsonObject(i).encodePrettily());
                }
                redisAPI
                    .sadd(paramList).onSuccess(r -> updateBottomNRankedItem());
                if (message != null) {
                    message.reply(value);
                }
            } else {
                System.out.println("Git:Redis GET/SET failed " + response.cause());
            }
        });
    }

    private void getAndSetInCacheAsKey(String pathName, Message<String> message) {
        vertx.eventBus().request(GITHUB_EVENT_ADDRESS, pathName, response -> {
            if (response.succeeded()) {
                final String value = response.result().body().toString();
                redisAPI
                        .set(List.of(pathName, value),
                                redisResult -> System.out.println("Redis set:key response" + redisResult.result()));

                if (message != null) {
                    message.reply(value);

                }
            } else {
                System.out.println("Git:Redis GET/SET failed " + response.cause());
            }
        });
    }

    private Handler<Message<String>> handleRedisRankRequest() {
        return msg -> {
            String pathName = msg.body();
            List<String> param = List.of(pathName.split("/"));
            int bottomN = Integer.parseInt(param.get(3));
            CustomViewsUtil.CUSTOM_VIEWS views = CustomViewsUtil.CUSTOM_VIEWS.valueOf(param.get(4));
            if (leaderBoardReady.get() == Boolean.FALSE) {
                handleRedisRefreshTask();
            }
            redisAPI
                    .zrange(List.of(views.setName, "0", String.valueOf(bottomN - 1), "WITHSCORES"))
                    .onFailure(t -> System.out.println("Redis zrange Response failed " + t))
                    .onSuccess(message -> {
                        if (message == null || message.toString().equals("[]")) {
                            msg.reply("leader board not yet ready");
                        } else {
                            System.out.println("Redis zrange Response:: " + message);
                            List<LeaderBoardOutputItem> outputItemList = getAsList(views.setName, message);
                            //sorted twice 1) by score (desc), 2) by Repo Full name
                            outputItemList.sort(Comparator.comparing(LeaderBoardOutputItem::getScore)
                                    .thenComparing(Comparator.comparing(LeaderBoardOutputItem::getRepoFullName).reversed()));
                            msg.reply(outputItemList.toString());
                        }
                    });

        };
    }

    private List<LeaderBoardOutputItem> getAsList(String setName, Response message) {
        final String[] result = message.toString().split("],");
        final List<LeaderBoardOutputItem> outputItemList = new ArrayList<>();
        for (String each : result) {
            String[] eachSplit = each.split(",");
            LeaderBoardOutputItem outputItem = getLeaderBoardOutputItem(setName, eachSplit);
            outputItemList.add(outputItem);
        }
        return outputItemList;
    }

    private LeaderBoardOutputItem getLeaderBoardOutputItem(String setName, String[] eachSplit) {
        LeaderBoardOutputItem outputItem;
        if (setName.equals(last_updated.setName)) {
            outputItem = new LeaderBoardOutputItem(eachSplit[0].substring(2), getScoreAsString(eachSplit));
        } else {
            outputItem = new LeaderBoardOutputItem(eachSplit[0].substring(2), getScoreAsInt(eachSplit));
        }
        return outputItem;
    }

    private int getScoreAsInt(String[] eachSplit) {
        return (int) Double.parseDouble(eachSplit[1].replace("]", "").trim());
    }

    private String getScoreAsString(String[] eachSplit) {
        return eachSplit[1].replace("]", "").trim();
    }
}
