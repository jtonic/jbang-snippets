///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//SOURCES SpringBootCassandraMain.java
//SOURCES ../jb/JBangJunit6Test.java
//DEPS org.springframework.boot:spring-boot-starter-test:4.0.8

package sbc;

import jb.JBangJunit6Test;
import sbc.SpringBootCassandraMain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = SpringBootCassandraMain.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SpringBootCassandraTCTest extends JBangJunit6Test {

    public static void main(String[] args) {
        JBangJunit6Test.run(SpringBootCassandraTCTest.class);
    }

    @LocalServerPort
    int port;

    @MockitoBean
    SpringBootCassandraMain.OAuth2ClientConfigRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void findAllReturnsAllConfigs() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                entity("client-app-1", "Payment Processing"),
                entity("client-app-2", "User Authentication")));

        HttpResponse<String> response = get("/oauth2-config");

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("client-app-1"));
        Assertions.assertTrue(response.body().contains("client-app-2"));
    }

    @Test
    void findByClientIdAndBusinessPurposeReturnsConfig() throws Exception {
        when(repository.findByKeyClientIdAndKeyBusinessPurpose("client-app-1", "Payment Processing"))
                .thenReturn(Optional.of(entity("client-app-1", "Payment Processing")));

        HttpResponse<String> response = get("/oauth2-config?clientId=client-app-1&businessPurpose=Payment%20Processing");

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("client-app-1"));
        verify(repository).findByKeyClientIdAndKeyBusinessPurpose("client-app-1", "Payment Processing");
    }

    @Test
    void findByClientIdAndBusinessPurposeNotFoundReturns404() throws Exception {
        when(repository.findByKeyClientIdAndKeyBusinessPurpose("unknown", "Whatever"))
                .thenReturn(Optional.empty());

        HttpResponse<String> response = get("/oauth2-config?clientId=unknown&businessPurpose=Whatever");

        Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    void createConfigPersistsAndReturnsDto() throws Exception {
        when(repository.doSave(any())).thenReturn(entity("client-app-9", "Audit Logging"));

        HttpResponse<String> response = post("/oauth2-config",
                //language=JSON
                """
                {
                  "oauth2": {
                    "clientId": "client-app-9",
                    "businessPurpose": "Audit Logging",
                    "responseMode": "direct_post",
                    "requestUriMethod": "post",
                    "redirectUri": "https://client9.example.com/redirect",
                    "verifierInfo": "verifier-9"
                  },
                  "version": "9.0"
                }
                """);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("client-app-9"));
        verify(repository).doSave(any());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private SpringBootCassandraMain.OAuth2ClientConfigEntity entity(String clientId, String businessPurpose) {
        SpringBootCassandraMain.OAuth2ClientConfigEntity entity = new SpringBootCassandraMain.OAuth2ClientConfigEntity();
        entity.setKey(new SpringBootCassandraMain.OAuth2ClientConfigKey(clientId, businessPurpose));
        entity.setVersion("1.0");
        entity.setResponseMode("direct_post");
        entity.setRequestUriMethod("post");
        entity.setRedirectUri("https://" + clientId + ".example.com/redirect");
        entity.setVerifierInfo("verifier-" + clientId);
        return entity;
    }
}