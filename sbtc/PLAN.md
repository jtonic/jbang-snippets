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

- [x] B. Spring Boot integration tests for this application, in a jbang test file next to the main one:
  - [x] B.1 WireMock stands in for the `sbc` application, so the HTTP calls made by `SpringBootTestContainersMain` run against WireMock:
    - a) new [SpringBootTestContainersTest.java](./SpringBootTestContainersTest.java), in the style of [SpringBootCassandraTCTest.java](../sbc/SpringBootCassandraTCTest.java)
      (`//SOURCES` of the main file and of `../jb/JBangJunit6Test.java`, `@SpringBootTest` on a random port),
    - b) an embedded `WireMockServer` (`org.wiremock:wiremock-standalone`) on a dynamic port,
    - c) `sbc.base-url` overridden with the WireMock base URL via `@DynamicPropertySource` — no change to the main file,
    - d) tests: upstream `200` JSON body is returned as is by `GET /client/oauth2-config`; an upstream error ends up as `500`,
    - e) check with `jbang SpringBootTestContainersTest.java`,
    - f) the [README.md](./README.md) says how to run the integration tests

- [x] C. A `users` feature backed by Cassandra, added to this application, with the integration tests running against a Cassandra test container.
  It is almost what we have in [SpringBootCassandraMain.java](../sbc/SpringBootCassandraMain.java), but for users, and everything lives in the [sbtc](.) folder:
  - [x] C.1 Cassandra as docker compose, as we have in [sbc/docker-compose.yml](../sbc/docker-compose.yml):
    - a) new [docker-compose.yml](./docker-compose.yml) with a single Cassandra service, plus its own `init-cassandra.sh` and [schema.cql](./schema.cql),
    - b) container name `sbtc-cassandra` and host port `9043` (mapped to `9042`), so it can run next to `sbc-cassandra`,
    - c) [schema.cql](./schema.cql) creates the keyspace `sbtc` and the table `users`
      (`ssn TEXT`, `first_name TEXT`, `last_name TEXT`, `email TEXT`, `PRIMARY KEY (ssn)`) and populates it with two rows,
    - d) validate with the docker commands and a `cqlsh` select run inside the running container
  - [x] C.2 The users feature goes in the existing [SpringBootTestContainersMain.java](./SpringBootTestContainersMain.java) — no separate main file.
    From now on the application needs Cassandra to start, both when run with `jbang` and in the integration tests:
    - a) add `spring-boot-starter-data-cassandra` (4.1.1) to the jbang metadata, next to the existing dependencies,
    - b) the existing [application.properties](./application.properties) gets the `spring.cassandra.*` properties:
      `localhost:9043`, keyspace `sbtc`, local datacenter `datacenter1` (server port stays `8081`),
    - c) `UserEntity` (`@Table("users")`, `ssn` as `@PrimaryKey`, `@Column` snake_case ↔ camelCase) with `toDomain()` / `from(User)` — the mapping stays on the entity, as in `sbc`,
    - d) `User` java record as domain model / DTO (`ssn`, `firstName`, `lastName`, `email`),
    - e) `UserRepository extends CassandraRepository<UserEntity, String>` with the `doSave` wrapper throwing a `UserPersistenceException`, handled by a `@RestControllerAdvice`,
    - f) `UserController` with three endpoints:
      - `POST /users` — add a user,
      - `GET /users` — get all users,
      - `GET /users/{ssn}` — get the user by ssn, `404` when not found
    - g) the existing `GET /client/oauth2-config` endpoint stays as it is,
    - h) [test.http](./test.http) gets the http commands for the three endpoints,
    - i) check with docker compose up, `jbang SpringBootTestContainersMain.java`, `curl` and `jq`
  - [x] C.3 Test container for Cassandra (former B.2), in the existing [SpringBootTestContainersTest.java](./SpringBootTestContainersTest.java).
    As the application no longer starts without Cassandra, the test class runs with both stand-ins: WireMock for `sbc` and a Cassandra test container:
    - a) `org.testcontainers:testcontainers-cassandra` — a `CassandraContainer` (same image as in docker compose), started once for the test class;
      the Spring Boot BOM (`spring-boot-dependencies:4.1.1@pom`) is the first `//DEPS`, otherwise an old `jackson-annotations` (2.7) wins and the context fails to start,
    - b) the container is initialised with the same [schema.cql](./schema.cql) used by docker compose (`//FILES schema.cql` + `withInitScript`) — a single source for the schema,
    - c) `spring.cassandra.contact-points` / `port` / `local-datacenter` overridden with the container's values in the existing `@DynamicPropertySource` — no test-only code in the main file,
    - d) the two WireMock tests of B.1 keep passing, unchanged,
    - e) new tests, through the HTTP endpoints only and against the real Cassandra (no mocked repository, unlike [SpringBootCassandraTCTest.java](../sbc/SpringBootCassandraTCTest.java)):
      - add a user (`POST /users`) and read it back,
      - get all users (`GET /users`) returns the seeded rows plus the added one,
      - get user by ssn (`GET /users/{ssn}`) returns the user; an unknown ssn returns `404`
    - f) check with `jbang SpringBootTestContainersTest.java` (Docker must be running; docker compose is not needed),
    - g) the [README.md](./README.md) is updated: Cassandra (docker compose in this folder) as a prerequisite to run the application, the users endpoints, and Docker as a prerequisite for the integration tests

More to come...
