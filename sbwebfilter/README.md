# sbwebfilter

A single-file Spring Boot 4 WebFlux application for [JBang](https://www.jbang.dev), written in Kotlin.
It shows how to apply web filters (request logging and an access token check) to the endpoints of one router,
with the beans wired by the Kotlin bean DSL instead of annotations.

## Quick guide

The only prerequisite is [mise](https://mise.jdx.dev/getting-started.html), which can be installed on macOS, Linux
and Windows. It provides the tools declared in [mise.toml](../mise.toml) (Java, JBang and Node.js among them);
JBang then downloads the Kotlin compiler and the dependencies itself.

Install the tools, once, from the root of the repository:

```shell
mise install
```

Run the application (it listens on port 8080):

```shell
jbang SbWebFilterMain.kt
```

Call it, with and without the access token:

```shell
TOKEN=$(grep '^@accessToken' sbwebfilter.http | sed 's/.*= //')

curl -i -H "X-KKK-AccessToken: $TOKEN" http://localhost:8080/users   # 200
curl -i http://localhost:8080/users                                  # 401
curl -i http://localhost:8080/employees                              # 200, no token needed
```

Run the requests of [sbwebfilter.http](sbwebfilter.http) together with their status code tests, either with
"Run All Requests in File" in IntelliJ IDEA, or from the command line with the
[IntelliJ HTTP Client CLI](https://www.jetbrains.com/help/idea/http-client-cli.html) (`ijhttp`), the same engine as the IDE:

```shell
# installed locally: with Homebrew, run `brew bundle` once from the root of the repository (see ../Brewfile);
# otherwise use the ZIP archive, which needs JDK 25 or newer
ijhttp sbwebfilter.http

# or with Docker, nothing to install; -D makes localhost point to the host machine
docker run --rm -v $PWD:/workdir jetbrains/intellij-http-client -D sbwebfilter.http
```

As a fallback when `ijhttp` is not available, [httpyac](https://httpyac.github.io) runs the same file. It is a separate
tool that supports the common part of the IntelliJ syntax, not all of it:

```shell
npx httpyac send sbwebfilter.http --all
```

## Tech stack

The versions are not repeated here. Those of the tools are in [mise.toml](../mise.toml); those of the language and
the libraries are in the `//KOTLIN` and `//DEPS` lines at the top of [SbWebFilterMain.kt](SbWebFilterMain.kt).

| What              | Version in           | Used for                                               |
|-------------------|----------------------|--------------------------------------------------------|
| mise              |                      | installs the tools                                     |
| Java              | `mise.toml`          | runtime and bytecode target                            |
| JBang             | `mise.toml`          | builds and runs the single-file application            |
| Node.js           | `mise.toml`          | `npx`, for the `httpyac` fallback of the `.http` tests |
| Kotlin            | `SbWebFilterMain.kt` | language                                               |
| Spring Boot       | `SbWebFilterMain.kt` | WebFlux on Netty, Kotlin bean DSL (`BeanRegistrarDsl`) |
| Kotlin coroutines | Spring Boot BOM      | `coRouter` and `CoWebFilter` (suspending handlers)     |
| Nimbus JOSE + JWT | `SbWebFilterMain.kt` | reading the content of the JWT                         |

## Endpoints

| Endpoint              | `X-KKK-AccessToken` | Answers                                       |
|-----------------------|---------------------|-----------------------------------------------|
| `GET /users`          | required            | 200 with the users, 401 without a readable JWT |
| `GET /users/{id}`     | required            | 200 with the user, 404 if unknown, 401 without a readable JWT |
| `GET /employees`      | not needed          | 200 with the employees                        |
| `GET /employees/{id}` | not needed          | 200 with the employee, 404 if unknown         |

## How it works

- **Bean DSL instead of annotations:** the only annotations are `@SpringBootApplication` and `@Import(Beans::class)`;
  every bean is registered in `Beans`, a `BeanRegistrarDsl`.
- **One `coRouter` per area:** `userRouter` and `employeeRouter` are separate router beans with suspending handlers.
- **Filters scoped to a router, without the `filter { }` DSL:** `LoggingFilter` and `AccessTokenFilter` are `CoWebFilter`
  beans. They ask `RouterEndpoints` whether the `userRouter` would handle the request, so a route added to that router
  is covered without touching the filters, and no path pattern is hard-coded.
- **Access token:** `AccessTokenFilter` reads the JWT from the `X-KKK-AccessToken` header with `JwtReader` and answers
  401 when it is missing or unreadable.
- **Token in the handlers:** a handler gets the parsed token with `request.accessToken()`, the functional-router
  counterpart of an `@AccessToken` method parameter of an annotated controller.

## Security note

The signature of the JWT is **not** checked: the application only reads the content of the token. It assumes that a
component in front of it (for example a sidecar container in the same pod) has verified the token and forwards the header
unchanged, and that the application is reachable only through that component. Exposed directly, it would accept a forged token.

## More

- [TODO.md](TODO.md): the steps done so far and the upcoming ones
- [CONTRIBUTING.md](CONTRIBUTING.md): the git flow for changes
