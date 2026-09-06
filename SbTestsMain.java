///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter-web:4.1.1
//DEPS org.springframework.boot:spring-boot-starter-test:4.1.1
//DEPS org.junit.jupiter:junit-jupiter:6.0.3
//DEPS org.junit.platform:junit-platform-launcher:6.0.3

package stonic.sb;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

@SpringBootApplication
public class SbTestsMain {

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("stonic.sb"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.printFailuresTo(new PrintWriter(System.out));

        long failed = summary.getTotalFailureCount();
        int exitCode = failed > 0 ? 1 : 0;
        System.out.printf("Tests found: %d, succeeded: %d, failed: %d%n",
                summary.getTestsStartedCount(), summary.getTestsSucceededCount(), failed);
        System.exit(exitCode);
    }

    @RestController
    static class ApiRestController {

        @GetMapping("/")
        String hello() {
            return "Hello from Spring Boot on JBang!";
        }
    }

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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