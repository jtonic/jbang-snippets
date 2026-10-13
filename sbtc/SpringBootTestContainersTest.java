///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//SOURCES SpringBootTestContainersMain.java
//SOURCES ../jb/JBangJunit6Test.java
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.1@pom
//DEPS org.springframework.boot:spring-boot-starter-test:4.1.1
//DEPS org.wiremock:wiremock-standalone:3.13.2
//DEPS org.testcontainers:testcontainers-cassandra:2.0.5
//FILES schema.cql

package sbtc;

import com.github.tomakehurst.wiremock.WireMockServer;
import jb.JBangJunit6Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(
        classes = SpringBootTestContainersMain.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class SpringBootTestContainersTest extends JBangJunit6Test {

    public static void main(String[] args) {
        JBangJunit6Test.run(SpringBootTestContainersTest.class);
    }

    // stands in for the sbc application
    private static final WireMockServer sbc = new WireMockServer(wireMockConfig().dynamicPort());

    // a real Cassandra, with the schema and the rows of the docker compose one
    private static final CassandraContainer cassandra = new CassandraContainer("cassandra:4.1")
            .withInitScript("schema.cql");

    @DynamicPropertySource
    static void sbcProperties(DynamicPropertyRegistry registry) {
        sbc.start();
        registry.add("sbc.base-url", sbc::baseUrl);
    }

    @DynamicPropertySource
    static void cassandraProperties(DynamicPropertyRegistry registry) {
        cassandra.start();
        registry.add("spring.cassandra.contact-points", () -> cassandra.getContactPoint().getHostString());
        registry.add("spring.cassandra.port", () -> cassandra.getContactPoint().getPort());
        registry.add("spring.cassandra.local-datacenter", cassandra::getLocalDatacenter);
    }

    @AfterAll
    static void stopSbc() {
        sbc.stop();
    }

    @AfterAll
    static void stopCassandra() {
        cassandra.stop();
    }

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void resetSbc() {
        sbc.resetAll();
    }

    @Test
    void getConfigsReturnsUpstreamBody() throws Exception {
        //language=JSON
        String upstreamBody = """
                [
                  {
                    "oauth2": {
                      "clientId": "client-app-1",
                      "businessPurpose": "Payment Processing",
                      "responseMode": "direct_post",
                      "requestUriMethod": "post",
                      "redirectUri": "https://client1.example.com/redirect",
                      "verifierInfo": "verifier-1"
                    },
                    "version": "1.0"
                  }
                ]
                """;
        sbc.stubFor(get(urlEqualTo("/oauth2-config"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(upstreamBody)));

        HttpResponse<String> response = httpGet("/client/oauth2-config");

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        Assertions.assertEquals(upstreamBody, response.body());
        sbc.verify(1, getRequestedFor(urlEqualTo("/oauth2-config")));
    }

    @Test
    void getConfigsUpstreamErrorReturns500() throws Exception {
        sbc.stubFor(get(urlEqualTo("/oauth2-config"))
                .willReturn(aResponse().withStatus(503)));

        HttpResponse<String> response = httpGet("/client/oauth2-config");

        Assertions.assertEquals(500, response.statusCode());
        sbc.verify(1, getRequestedFor(urlEqualTo("/oauth2-config")));
    }

    @Test
    void addUserPersistsAndReturnsUser() throws Exception {
        HttpResponse<String> created = httpPost("/users",
                //language=JSON
                """
                {
                  "ssn": "333-33-3333",
                  "firstName": "Grace",
                  "lastName": "Hopper",
                  "email": "grace.hopper@example.com"
                }
                """);

        Assertions.assertEquals(200, created.statusCode());
        Assertions.assertTrue(created.body().contains("333-33-3333"));

        HttpResponse<String> found = httpGet("/users/333-33-3333");

        Assertions.assertEquals(200, found.statusCode());
        Assertions.assertTrue(found.body().contains("\"firstName\":\"Grace\""));
        Assertions.assertTrue(found.body().contains("grace.hopper@example.com"));
    }

    @Test
    void getUsersReturnsSeededAndAddedUsers() throws Exception {
        httpPost("/users",
                //language=JSON
                """
                {
                  "ssn": "444-44-4444",
                  "firstName": "Edsger",
                  "lastName": "Dijkstra",
                  "email": "edsger.dijkstra@example.com"
                }
                """);

        HttpResponse<String> response = httpGet("/users");

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("111-11-1111"));
        Assertions.assertTrue(response.body().contains("222-22-2222"));
        Assertions.assertTrue(response.body().contains("444-44-4444"));
    }

    @Test
    void getUserBySsnReturnsUser() throws Exception {
        HttpResponse<String> response = httpGet("/users/111-11-1111");

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("\"ssn\":\"111-11-1111\""));
        Assertions.assertTrue(response.body().contains("\"firstName\":\"Ada\""));
    }

    @Test
    void getUserByUnknownSsnReturns404() throws Exception {
        HttpResponse<String> response = httpGet("/users/000-00-0000");

        Assertions.assertEquals(404, response.statusCode());
    }

    private HttpResponse<String> httpGet(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> httpPost(String path, String body) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
