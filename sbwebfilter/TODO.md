# PLAN

This is going to create a SbWebFilterMain application (Kotlin file) for jbang: a Spring Boot 4 WebFlux application
wired with the Kotlin bean DSL and coroutine routers, showing how a web filter is applied to the endpoints of a router.

1. [x] Create a Spring Boot 4 WebFlux application with coroutine routers and a logging web filter
    - [x] Use the Spring Boot 4.1.1 BOM, `spring-boot-starter-webflux` and `kotlinx-coroutines-reactor` as dependencies
    - [x] Keep annotations at the minimum: only `@SpringBootApplication` and `@Import(Beans::class)`,
      everything else is registered in a `BeanRegistrarDsl` (`Beans`)
    - [x] Create `UserService` and `EmployeeService` beans holding the sample data
    - [x] Create one `coRouter` bean per area:
      a) `userRouter` with `GET /users` and `GET /users/{id}`
      b) `employeeRouter` with `GET /employees` and `GET /employees/{id}`
      c) an unknown id returns 404
    - [x] Create `LoggingFilter` as a `CoWebFilter` bean (not as the `filter { }` DSL of a router) that:
      a) is applied on all the endpoints of the `userRouter`, picked up from the router itself (no hard-coded path patterns)
      b) logs through SLF4J the remote address, the HTTP method, the path and the response status
    - [x] Add [sbwebfilter.http](sbwebfilter.http) with requests for all the endpoints
    - [x] Run a code review and address its findings: the stray pasted path, `println` replaced by a logger,
      the http file renamed, and `-jvm-target=25` checked to be applied (class-file version 69)
   Validated by starting the application with `jbang` and using `curl`: the endpoints of the `userRouter` are logged,
   while the endpoints of the `employeeRouter` and the unrouted paths are not.
2. [x] Add a JWT library to parse a signed JWT
    - [x] Add the JWT library (Nimbus JOSE + JWT) as a jbang dependency
    - [x] Parse a signed JWT without checking its signature: only read its content (`JwtReader`)
    - [x] Expose the body of the JWT, including the claims and Co (header, subject, issuer, expiration, custom claims)
      as an `AccessToken`
3. [x] Check in the endpoints of the `userRouter` if the `X-KKK-AccessToken` header is present
    - [x] Create `AccessTokenFilter` as a `CoWebFilter` bean applied on the endpoints of the `userRouter`,
      sharing the endpoints pick-up with `LoggingFilter` (`RouterEndpoints`)
    - [x] The header carries the JWT: it is read with `JwtReader`, logged and stored as an exchange attribute
    - [x] Return 401 when the header is missing or does not carry a readable JWT
    - [x] Expose the `AccessToken` to the handlers of the endpoints, keeping the `coRouter` (no annotated controller,
      so no `@AccessToken` method parameter): a `ServerRequest.accessToken()` extension, used by the handlers of
      the `userRouter` to log who asked
   Validated by starting the application with `jbang` and using `curl`: the endpoints of the `userRouter` answer 401
   without the header or with a garbage token and work with a JWT, while the endpoints of the `employeeRouter` need no header.
4. [x] Apply `LoggingFilter` on all the endpoints of all the routers
    - [x] `RouterEndpoints` takes a list of routers: `allEndpoints` (every router bean) for `LoggingFilter`,
      `userEndpoints` (the `userRouter` only) for `AccessTokenFilter`
    - [x] Make the order of the filters explicit in the DSL (`registerBean(order = ...)`): `LoggingFilter` first,
      so the 401 responses are logged too
   Validated by starting the application with `jbang` and using `curl`: the endpoints of both routers are logged,
   the unrouted paths are not, and only the endpoints of the `userRouter` need the access token.
5. More to come
