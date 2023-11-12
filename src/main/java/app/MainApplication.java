package app;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import server.CachingService;

import javax.annotation.PostConstruct;

@SpringBootApplication
@ComponentScan("server")
public class MainApplication {

    @Autowired
    private CachingService serverVerticle;

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
        System.out.println("Netflix Git Repo Caching App Started");
    }

    @PostConstruct
    public void deployVerticles() {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(serverVerticle, response -> {
            if(response.succeeded()) {
                System.out.println("Service Router Deployed");
            } else {
                System.out.println("Service Router Deployment Failed" + response.cause());
            }
        });
    }
}
