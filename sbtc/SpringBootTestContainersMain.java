///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter-web:4.1.1
//DEPS org.springframework.boot:spring-boot-starter-restclient:4.1.1
//DEPS org.slf4j:slf4j-api:2.0.17
//FILES application.properties

package sbtc;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SpringBootTestContainersMain {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestContainersMain.class, args);
    }

    @Bean
    RestClient sbcRestClient(RestClient.Builder builder, @Value("${sbc.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Component
    static class OAuth2ClientConfigClientService {

        private final RestClient sbcRestClient;
        private final AsyncTaskExecutor executor;

        OAuth2ClientConfigClientService(RestClient sbcRestClient,
                @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor) {
            this.sbcRestClient = sbcRestClient;
            this.executor = executor;
        }

        CompletableFuture<String> fetchConfigs() {
            return executor.submitCompletable(() -> sbcRestClient.get()
                    .uri("/oauth2-config")
                    .retrieve()
                    .body(String.class));
        }
    }

    @RestController
    static class OAuth2ClientConfigClientController {

        private final OAuth2ClientConfigClientService service;

        OAuth2ClientConfigClientController(OAuth2ClientConfigClientService service) {
            this.service = service;
        }

        @GetMapping(value = "/client/oauth2-config", produces = MediaType.APPLICATION_JSON_VALUE)
        CompletableFuture<String> getConfigs() {
            return service.fetchConfigs();
        }
    }
}
