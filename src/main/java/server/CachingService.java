package server;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import utils.ApiPathUtil;

import java.util.Arrays;

import static utils.ApiPathUtil.CACHED_PAGINATED_GET;
import static utils.ApiPathUtil.CUSTOM_GET;
import static utils.ApiPathUtil.HEALTH_CHECK_PATH;
import static utils.ApiPathUtil.CACHED_GET;
import static utils.ApiPathUtil.PROXY_GET;


@Component
@NoArgsConstructor
public class
CachingService extends AbstractVerticle {

    public CachingService(Vertx vertx) {
        this.vertx = vertx;
    }


    @Override
    public void start() throws Exception {
        super.start();
        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());// Global Handler to generate Routing context

        // 1. Add /healthCheck path
        addHealthCheckRoute(router);

        // 2. Add all Cached Route
        addCachedGETRoute(router, ApiPathUtil.PROXY_PATH_REFRESH.values());

        // 2. Add all Cached Route required Pagination
        addCachedPaginatedGETRoute(router, ApiPathUtil.PROXY_PATH_PAGINATED_REFRESH.values());

        // 3. Add all Custom View Route
        addCustomViewGETRoute(router, ApiPathUtil.VIEW_PATH.values());

        // 4. Add any Proxy Route
        addProxyGETRoute(router);

        ConfigStoreOptions env = new ConfigStoreOptions()
                .setType("env")
                .setConfig(new JsonObject().put("keys", new JsonArray().add("APP_PORT")));

        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(env));
        retriever.getConfig().onComplete(config -> {
            HttpServer httpServer = vertx.createHttpServer()
                    .requestHandler(router)
                    .exceptionHandler(exception -> {
                        System.out.println("Failed to start the http webserver" + exception.getCause());
                    })
                    .connectionHandler(res -> {
                        System.out.println("Received a new Connection " + res.indicatedServerName());
                    });
            final int port = config.succeeded() && config.result() != null && config.result().getInteger("APP_PORT") != null ? config.result().getInteger("APP_PORT"): 8080;
            System.out.println("Vertx Web Server to start on port " + port);
            httpServer.listen(config.result().getInteger("APP_PORT"));
            deployRequestDispatcherVerticle();
        });
    }

    private void addProxyGETRoute(Router router) {
        Route route = router.route().method(HttpMethod.GET);
        route.handler(routingContext -> {
            vertx.eventBus().request(PROXY_GET, routingContext.normalizedPath(), response -> {
                if (response.succeeded()) {
                    routingContext.response()
                            .setStatusCode(HttpStatus.OK.value())
                            .end(response.result().body().toString());
                } else {
                    routingContext.response()
                            .setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                            .setStatusMessage(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                            .end();
                }
            });
        });
    }

    private void addHealthCheckRoute(Router router) {
        router.get(HEALTH_CHECK_PATH).handler(routingContext -> routingContext.response()
                .setStatusCode(HttpStatus.OK.value())
                .setStatusMessage(HttpStatus.OK.getReasonPhrase())
                .end("Live"));
    }

    private void deployRequestDispatcherVerticle() {
        vertx.deployVerticle(RequestRouteDispatcher::new,
                new DeploymentOptions()
                        .setWorker(true)
                        .setWorkerPoolName("API-Router")
                        .setWorkerPoolSize(20),
                res -> {
                    System.out.println("Deployed RequestRouteDispatcher-Verticle" + res.result());
                });
    }

    private void addCustomViewGETRoute(Router router, ApiPathUtil.VIEW_PATH[] customViewPathList) {
        Arrays.stream(customViewPathList).iterator().forEachRemaining(apiPath -> router.get(apiPath.value)
                .handler(this::handleCustomViewGetRequest));
    }

    private void addCachedGETRoute(Router router, ApiPathUtil.PROXY_PATH_REFRESH[] proxyPathList) {
        Arrays.stream(proxyPathList).iterator().forEachRemaining(apiPath -> router.get(apiPath.value)
                .handler(this::handleCachedGetRequest));
    }
    private void addCachedPaginatedGETRoute(Router router, ApiPathUtil.PROXY_PATH_PAGINATED_REFRESH[] proxyPathList) {
        Arrays.stream(proxyPathList).iterator().forEachRemaining(apiPath -> router.get(apiPath.value)
                .handler(this::handleCachedPaginatedGetRequest));
    }

    private void handleCachedGetRequest(RoutingContext routingContext) {
        vertx.eventBus().request(CACHED_GET, routingContext.normalizedPath(), response -> {
            if (response.succeeded()) {
                routingContext.response()
                        .setStatusCode(HttpStatus.OK.value())
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(new JsonObject(response.result().body().toString()).encodePrettily());
            } else {
                routingContext.response()
                        .setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .setStatusMessage(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                        .end();
            }
        });
    }

    private void handleCachedPaginatedGetRequest(RoutingContext routingContext) {
        vertx.eventBus().request(CACHED_PAGINATED_GET, routingContext.normalizedPath(), response -> {
            if (response.succeeded()) {
                routingContext.response()
                        .setStatusCode(HttpStatus.OK.value())
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(new JsonArray(response.result().body().toString()).encodePrettily());
            } else {
                routingContext.response()
                        .setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .setStatusMessage(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                        .end();
            }
        });
    }

    private void handleCustomViewGetRequest(RoutingContext routingContext) {
        vertx.eventBus().request(CUSTOM_GET,routingContext.normalizedPath(), response -> {
            if (response.succeeded()) {
                routingContext.response()
                        .setStatusCode(HttpStatus.OK.value())
                        .end(response.result().body().toString());
            } else {
                routingContext.response()
                        .setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .setStatusMessage(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                        .end();
            }
        });
    }
}
