///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//KOTLIN 2.4.10
//COMPILE_OPTIONS -jvm-target=25
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.1@pom
//DEPS org.springframework.boot:spring-boot-starter-webflux
//DEPS org.jetbrains.kotlinx:kotlinx-coroutines-reactor
//FILES application.properties

package sbwebfilter

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.http.codec.HttpMessageReader
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

private val log = LoggerFactory.getLogger("sbwebfilter.LoggingFilter")

@SpringBootApplication
@Import(Beans::class)
class SbWebFilterMain

class Beans : BeanRegistrarDsl({
    registerBean<UserService>()
    registerBean<EmployeeService>()

    registerBean("employeeRouter") {
        val employeeService = bean<EmployeeService>()

        coRouter {
            GET("/employees") { ok().bodyValueAndAwait(employeeService.getEmployees()) }
            GET("/employees/{id}") { request ->
                employeeService.getEmployee(request.pathVariable("id"))
                    ?.let { ok().bodyValueAndAwait(it) }
                    ?: notFound().buildAndAwait()
            }
        }
    }

    registerBean("userRouter") {
        val userService = bean<UserService>()

        coRouter {
            GET("/users") { ok().bodyValueAndAwait(userService.getUsers()) }
            GET("/users/{id}") { request ->
                userService.getUser(request.pathVariable("id"))
                    ?.let { ok().bodyValueAndAwait(it) }
                    ?: notFound().buildAndAwait()
            }
        }
    }

    registerBean {
        LoggingFilter(
            router = bean<RouterFunction<ServerResponse>>("userRouter"),
            messageReaders = bean<ServerCodecConfigurer>().readers,
        )
    }
})

// Applies to every endpoint declared by the given router; other paths pass through unlogged.
class LoggingFilter(
    private val router: RouterFunction<ServerResponse>,
    private val messageReaders: List<HttpMessageReader<*>>,
) : CoWebFilter() {

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        // match on a copy: routing writes path attributes that must not leak into the real exchange
        val probe = ServerRequest.from(ServerRequest.create(exchange, messageReaders)).build()
        val routed = router.route(probe).awaitSingleOrNull() != null

        chain.filter(exchange)

        if (routed) {
            log.info(
                "{} requested {} {} -> {}",
                exchange.request.remoteAddress?.address?.hostAddress,
                exchange.request.method,
                exchange.request.path,
                exchange.response.statusCode?.value(),
            )
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SbWebFilterMain>(*args)
}

class UserService {

    private val users =
        listOf(
            User(id = "1", username = "user1"),
            User(id = "2", username = "user2"),
        )

    fun getUsers() = users

    fun getUser(id: String) = users.find { it.id == id }
}

class EmployeeService {

    private val employees =
        listOf(
            Employee(id = "1", name = "employee1"),
            Employee(id = "2", name = "employee2"),
        )

    fun getEmployees() = employees

    fun getEmployee(id: String) = employees.find { it.id == id }
}

data class Employee(val id: String, val name: String)

data class User(val id: String, val username: String)
