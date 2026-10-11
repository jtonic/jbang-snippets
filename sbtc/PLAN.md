# Spring Boot Test Containers

This is going to create a SpringBootTestContainersMain application (Java file) for jbang,
a small Spring Boot client of the [sbc](../sbc/SpringBootCassandraMain.java) application.

- [x] A. Considering [this example](../sbc/SpringBootCassandraMain.java) as an example for the jbang metadata at the start of the file:
  - [x] A.1 Use Java 25 (`//JAVA 25`),
  - [x] A.2 Use the latest GA Spring Boot (4.1.1 at the time of writing) with `spring-boot-starter-web` and `spring-boot-starter-restclient` as the only Spring Boot dependencies (no Spring Data / Cassandra),
  - [x] A.3 Enable virtual threads in Spring Boot (`spring.threads.virtual.enabled=true`),
  - [x] A.4 Keep `slf4j-api` and `//FILES application.properties` as in the example,
  - [x] A.5 Create a single Java file with only:
    - a) the Spring Boot main application class (`@SpringBootApplication`),
    - b) a single `@RestController` nested in it,
    - c) a service annotated with `@Component`, nested in it too, that holds the HTTP call (`fetchConfigs`); the controller only delegates to it
  - [x] A.6 The controller has only one endpoint, `GET /client/oauth2-config`, that:
    - a) consumes `GET /oauth2-config` exposed by the `sbc` application,
    - b) uses Spring's `RestClient`, built from the auto-configured `RestClient.Builder` and exposed as a `@Bean` with the `sbc` base URL — no RestTemplate / WebClient / raw JDK HttpClient,
    - c) wraps the (blocking) `RestClient` call in a `CompletableFuture`, run on the auto-configured virtual-thread `AsyncTaskExecutor` (`submitCompletable`),
    - d) has `CompletableFuture<...>` as return type of both the HTTP call and the controller method (Spring MVC completes the response asynchronously),
    - e) returns the upstream JSON body to its own caller
  - [x] A.7 Any needed configuration goes in a new application.properties in the [sbtc](.) folder:
    - a) the base URL of the `sbc` application,
    - b) a server port different from the one used by `sbc`, so both can run at the same time
  - [x] A.8 Add a [test.http](./test.http) with the http command for the new endpoint
  - [x] A.9 Check a successful implementation of this step by starting `sbc` (docker compose up + `jbang`), starting this application with `jbang`,
    and using `curl` and `jq` to see if the data from cassandra are returned through `/client/oauth2-config` to the cURL invoker.
  - [x] A.10 Add a [README.md](./README.md), in the style of [sbwebfilter/README.md](../sbwebfilter/README.md), with at least:
    - a) a short description of the application,
    - b) the prerequisites: the tools (mise, JBang, Docker) and, above all, the `sbc` application up and running —
      Cassandra via docker compose in the [sbc](../sbc) folder, then `jbang SpringBootCassandraMain.java`,
    - c) how to run this application with `jbang` (and on which port it listens),
    - d) how to test it: the `curl` / `jq` commands and the [test.http](./test.http) file,
    - e) the endpoint(s) and a brief "how it works" (`RestClient`, virtual threads, `CompletableFuture`)

More to come...
