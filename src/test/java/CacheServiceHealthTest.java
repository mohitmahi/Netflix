import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import model.LeaderBoardEntryItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import server.CachingService;

import static utils.ApiPathUtil.HEALTH_CHECK_PATH;

@ExtendWith(MockitoExtension.class)
@ExtendWith({VertxExtension.class
})
public class CacheServiceHealthTest {

    @Test
    @Order(1)
    void start_server(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(CachingService.class.getName(), res -> { if(res.succeeded()) {
            System.out.println("Deployed a new server Verticle" + res.result());
        } else {
            System.out.println("Failed server Verticle" + res.result());
        }
        });

        WebClientOptions options = new WebClientOptions().setUserAgent("My-App/1.2.3");
        options.setKeepAlive(false);
        WebClient webClient = WebClient.wrap(vertx.createHttpClient());

        LeaderBoardEntryItem input  = LeaderBoardEntryItem.builder().build();
        webClient.get(8080, "localhost", HEALTH_CHECK_PATH)
                .sendJsonObject(new JsonObject(Json.encode(input)),
                        testContext.succeeding(response -> {
                            testContext.verify(() ->
                                    Assertions.assertAll(
                                            () -> Assertions.assertEquals(200, response.statusCode()),
                                            () -> Assertions.assertEquals("OK", response.statusMessage())
                                    )
                            );
                            testContext.completeNow();
                        })
                );
    }
}
