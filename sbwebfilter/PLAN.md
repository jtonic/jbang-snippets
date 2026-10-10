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
