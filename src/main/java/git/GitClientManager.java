package git;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.redis.client.Response;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static utils.ApiPathUtil.CACHED_PAGINATED_GET;

/**
 * GitClientManager is a worker verticle with its own dedicated worker thread pool, which will listen to event bus
 * only for GITHUB_EVENT_ADDRESS and dispatch incoming request to external GITHUB_API_URL and return api response.
 */
@Component
public class GitClientManager extends AbstractVerticle {

    public static final String GITHUB_API_URL = "api.github.com";
    public static final String GITHUB_API_TOKEN = "GITHUB_API_TOKEN";
    public static final String GITHUB_EVENT_ADDRESS = "gitAPI";

    private WebClient client;
    private String apiToken;

    @Override
    public void start() {
        ConfigStoreOptions env = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray().add(GITHUB_API_TOKEN)));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(env));
        retriever.getConfig().onComplete(config -> apiToken = config.succeeded()
                && config.result() != null
                && !config.result().getString(GITHUB_API_TOKEN).isBlank() ?
                config.result().getString(GITHUB_API_TOKEN) : "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");
        vertx.eventBus().<String>consumer(GITHUB_EVENT_ADDRESS).handler(handleGitAPIRequest());
        WebClientOptions options = new WebClientOptions()
                .setUserAgent("Cache-App/1");
        options.setKeepAlive(false);
        client = WebClient.create(vertx, options);
    }

    private Handler<Message<String>> handleGitAPIRequest() {

        return msg -> {
            System.out.println("Calling GIT for " + msg.body());
            // Send a GET request to GITHUB_API_URL and publish an event to continue with next page in case of a paginated result
            client
                    .get(GITHUB_API_URL, msg.body())
                    .putHeader("Authorization", "Bearer " + apiToken)
                    .send()
                    .onSuccess(response -> {
                        AtomicReference<String> link = new AtomicReference<>(response.getHeader("link"));
                        if (Strings.isNotBlank(link.get())) {
                            String[] pageSplit = link.get().split(",");
                            for (String nextPage : pageSplit) {
                                if (nextPage.contains("next")) {
                                    System.out.println("Next Page link found " + nextPage);
                                    final String nextPageURL = nextPage.split(">")[0].substring(1);
                                    String validURL = nextPageURL.replace("<", "");
                                    final String path = validURL.substring(22).trim();
                                    vertx.eventBus().request(CACHED_PAGINATED_GET, path, result -> {
                                        if (result.succeeded()) {
                                            System.out.println("Next Page call succeeded for " + path + " :: " + result.succeeded());
                                        } else {
                                            System.out.println("Next Page call fauiled for " + path + " :: " + result.cause());
                                        }
                                    });
                                }
                            }
                            System.out.println("Received a paged response with status code " + msg.body() + " :: " + response.statusCode() + " link " + link);
                        } else {
                            System.out.println("Received all response with status code " + msg.body() + " :: " + response.statusCode());
                        }
                        msg.reply(response.bodyAsString());
                    })
                    .onFailure(err -> {
                        System.out.println("Something went wrong " + err.getMessage());
                        msg.reply("Failed to connect to Git" + err.getCause());
                    });

        };
    }
}
