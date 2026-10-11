# sbtc

A single-file Spring Boot 4 Web MVC application for [JBang](https://www.jbang.dev), written in Java.
It is a small client of the [sbc](../sbc/SpringBootCassandraMain.java) application: its only endpoint calls
`GET /oauth2-config` of `sbc` and returns the answer to its own caller.

## Prerequisites

- [mise](https://mise.jdx.dev/getting-started.html), which provides the tools declared in [mise.toml](../mise.toml)
  (Java and JBang among them). Install them, once, from the root of the repository:

  ```shell
  mise install
  ```

- Docker, for the Cassandra database of the `sbc` application.
- `curl` and `jq`, for the tests from the command line.
- **The `sbc` application up and running** on port 8080. From the [sbc](../sbc) folder, start Cassandra, wait until
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

The port and the address of the `sbc` application are in [application.properties](application.properties)
(`server.port` and `sbc.base-url`).

## Test

Call the endpoint; it answers with the same JSON as the `sbc` application:

```shell
curl -s http://localhost:8081/client/oauth2-config | jq .

# the same data, straight from the sbc application
curl -s http://localhost:8080/oauth2-config | jq .
```

Or run the request of [test.http](test.http) in IntelliJ IDEA or VS Code.

When done, stop both applications with `Ctrl+C` and, from the [sbc](../sbc) folder, stop Cassandra:

```shell
docker compose down -v
```

## Endpoints

| Endpoint                    | Answers                                                         |
|-----------------------------|-----------------------------------------------------------------|
| `GET /client/oauth2-config` | 200 with the OAuth2 client configurations returned by `sbc`     |

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

## More

- [PLAN.md](PLAN.md): the steps done so far and the upcoming ones
