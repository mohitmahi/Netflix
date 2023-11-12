package server;

import git.GitClientManager;
import cache.RedisClientManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.springframework.stereotype.Component;


import static cache.RedisClientManager.REDIS_SET_EVENT_ADDRESS;
import static git.GitClientManager.GITHUB_EVENT_ADDRESS;
import static cache.RedisClientManager.REDIS_MAP_EVENT_ADDRESS;
import static cache.RedisClientManager.REDIS_RANK_EVENT_ADDRESS;
import static utils.ApiPathUtil.CACHED_PAGINATED_GET;
import static utils.ApiPathUtil.CUSTOM_GET;
import static utils.ApiPathUtil.CACHED_GET;
import static utils.ApiPathUtil.PROXY_GET;

@Component
public class RequestRouteDispatcher extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();

        vertx.eventBus().<String>consumer(CUSTOM_GET).handler(handleCustomViewRequest());
        vertx.eventBus().<String>consumer(CACHED_GET).handler(handleCachedRequest());
        vertx.eventBus().<String>consumer(CACHED_PAGINATED_GET).handler(handlePaginagtedCachedRequest());
        vertx.eventBus().<String>consumer(PROXY_GET).handler(handleProxyRequest());

        deployRedisClientVerticle();
        deployGitClientVerticle();
    }

    private Handler<Message<String>> handleProxyRequest() {
        return getMessageHandler(GITHUB_EVENT_ADDRESS);
    }

    private Handler<Message<String>> handleCustomViewRequest() {
        return getMessageHandler(REDIS_RANK_EVENT_ADDRESS);
    }

    private Handler<Message<String>> handleCachedRequest() {
        return getMessageHandler(REDIS_MAP_EVENT_ADDRESS);
    }

    private Handler<Message<String>> handlePaginagtedCachedRequest() {
        return getMessageHandler(REDIS_SET_EVENT_ADDRESS);
    }

    private Handler<Message<String>> getMessageHandler(String redisEventAddress) {
        return msg -> {
            String pathName = msg.body();
            vertx.eventBus().request(redisEventAddress, pathName, response -> {
                if (response.succeeded()) {
                    msg.reply(response.result().body().toString());
                } else {
                    msg.reply("ERROR " + response.cause());
                }
            });

        };
    }

    private void deployRedisClientVerticle() {
        vertx.deployVerticle(RedisClientManager::new,
                new DeploymentOptions()
                        .setWorker(true)
                        .setWorkerPoolName("Cache-Read")
                        .setWorkerPoolSize(20),
                res -> {
                    System.out.println("Deployed Cache-Read-Verticle" + res.cause());
                });
    }

    private void deployGitClientVerticle() {
        vertx.deployVerticle(GitClientManager::new,
                new DeploymentOptions()
                        .setWorker(true)
                        .setWorkerPoolName("Git-api")
                        .setWorkerPoolSize(20),
                res -> {
                    System.out.println("Deployed Git-API-Verticle" + res.cause());
                });
    }
}
