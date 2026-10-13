# sbtc

A single-file Spring Boot 4 Web MVC application for [JBang](https://www.jbang.dev), written in Java.
It does two things:

- it is a small client of the [sbc](../sbc/SpringBootCassandraMain.java) application: `GET /client/oauth2-config` calls
  `GET /oauth2-config` of `sbc` and returns the answer to its own caller,
- it keeps users in its own Cassandra table (`sbtc.users`): add a user, get all users, get a user by ssn.

## Prerequisites

- [mise](https://mise.jdx.dev/getting-started.html), which provides the tools declared in [mise.toml](../mise.toml)
  (Java and JBang among them). Install them, once, from the root of the repository:

  ```shell
  mise install
  ```

- Docker, for the Cassandra databases (the one of this application and the one of `sbc`) and for the integration tests.
- `curl` and `jq`, for the tests from the command line.
- **Cassandra up and running** on port 9043, with the `sbtc` keyspace. The application does not start without it.
  From this folder, start it and wait until the container is healthy (the schema and the two initial users of
  [schema.cql](schema.cql) are loaded at its creation):

  ```shell
  docker compose up -d
  docker compose ps                    # wait for the "healthy" status
  docker exec sbtc-cassandra cqlsh -e "SELECT * FROM sbtc.users"
  ```

- **The `sbc` application up and running** on port 8080, only for the `/client/oauth2-config` endpoint. From the [sbc](../sbc) folder, start Cassandra, wait until
  the container is healthy (the schema and the two initial rows are loaded at its creation), then start the application:

  ```shell
  cd ../sbc
  docker compose up -d
  docker compose ps                    # wait for the "healthy" status
  jbang SpringBootCassandraMain.java
  ```

## Run

In another terminal, from this folder (the application listens on port 8081):

```shell
jbang SpringBootTestContainersMain.java
```

The port, the address of the `sbc` application and the Cassandra connection are in
[application.properties](application.properties) (`server.port`, `sbc.base-url` and `spring.cassandra.*`).

## Test

Call the endpoint; it answers with the same JSON as the `sbc` application:

```shell
curl -s http://localhost:8081/client/oauth2-config | jq .

# the same data, straight from the sbc application
curl -s http://localhost:8080/oauth2-config | jq .
```

The users endpoints:

```shell
# all users
curl -s http://localhost:8081/users | jq .

# one user, by ssn (404 when there is none)
curl -s http://localhost:8081/users/111-11-1111 | jq .

# add a user
curl -s -X POST http://localhost:8081/users \
  -H 'Content-Type: application/json' \
  -d '{"ssn":"333-33-3333","firstName":"Grace","lastName":"Hopper","email":"grace.hopper@example.com"}' | jq .
```

Or run the requests of [test.http](test.http) in IntelliJ IDEA or VS Code.

When done, stop both applications with `Ctrl+C` and stop Cassandra, in this folder and in the [sbc](../sbc) one:

```shell
docker compose down -v
```

## Integration tests

[SpringBootTestContainersTest.java](SpringBootTestContainersTest.java) is a `@SpringBootTest` that starts this
application on a random port, with two stand-ins:

- an embedded [WireMock](https://wiremock.org) server for the `sbc` application,
- a Cassandra [test container](https://testcontainers.com), created from the same [schema.cql](schema.cql) as the
  docker compose one, for the users.

So Docker has to be running, but neither `sbc` nor the docker compose Cassandra is needed. From this folder:

```shell
jbang SpringBootTestContainersTest.java
```

The run ends with a summary line and exits with a non-zero code if a test failed:

```text
Tests found: 6, succeeded: 6, failed: 0
```

The stack trace of a `503 Service Unavailable` in the output is expected: one of the tests makes WireMock answer
with an error, to check that it ends up as a `500`.

## Endpoints

| Endpoint                    | Answers                                                         |
|-----------------------------|-----------------------------------------------------------------|
| `GET /client/oauth2-config` | 200 with the OAuth2 client configurations returned by `sbc`     |
| `GET /users`                | 200 with all the users                                          |
| `GET /users/{ssn}`          | 200 with the user, 404 when there is no user with that ssn      |
| `POST /users`               | 200 with the added user (an existing ssn is overwritten)        |

## How it works

- **`RestClient`:** the HTTP client is Spring's `RestClient`, a bean built from the auto-configured
  `RestClient.Builder` with the base URL of the `sbc` application.
- **Virtual threads:** enabled with `spring.threads.virtual.enabled=true`.
- **Service and controller:** the HTTP call is in `OAuth2ClientConfigClientService`, a `@Component`; the
  `@RestController` only delegates to it.
- **`CompletableFuture`:** `RestClient` is blocking, so the service submits the call to the auto-configured
  `applicationTaskExecutor` with `submitCompletable`. The controller method returns the `CompletableFuture`
  and Spring MVC completes the response asynchronously.
- **Pass-through body:** the JSON of `sbc` is returned as it is, without mapping it to Java types.
- **Users:** `UserEntity` maps the `users` table (`ssn` is the primary key) and converts to and from the `User`
  record; `UserRepository` is a Spring Data `CassandraRepository`, whose `doSave` turns a failed save into a
  `UserPersistenceException`, answered as a `500` by the `@RestControllerAdvice`.
- **Integration tests:** WireMock listens on a dynamic port and the Cassandra test container on a mapped one; the test
  overrides `sbc.base-url` and `spring.cassandra.*` with their addresses through `@DynamicPropertySource`, so the
  application under test is the unchanged main file. The Spring Boot BOM in the `//DEPS` of the test keeps the
  Jackson 2 jars needed by Testcontainers at the versions Spring Boot expects.

## More

- [PLAN.md](PLAN.md): the steps done so far and the upcoming ones
