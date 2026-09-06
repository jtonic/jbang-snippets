///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//SOURCES SbAppMain.java
//SOURCES ../../jb/JBangJunit6Test.java
//DEPS org.springframework.boot:spring-boot-starter-test:4.1.1

package stonic.sb;

import jb.JBangJunit6Test;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class SbTestsMain extends JBangJunit6Test {

    public static void main(String[] args) {
        JBangJunit6Test.run(SbTestsMain.class, args);
    }

    @Override
    protected LauncherDiscoveryRequest discoveryRequest() {
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("stonic.sb"))
                .build();
    }

    @SpringBootTest(
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    static class EndpointTest {

        @LocalServerPort
        int port;

        @Test
        void helloEndpointReturnsGreeting() throws Exception {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Hello from Spring Boot on JBang!", response.body());
        }
    }
}