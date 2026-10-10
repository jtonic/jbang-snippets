///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//KOTLIN 2.4.10
//COMPILE_OPTIONS -jvm-target=25
//DEPS org.springframework.boot:spring-boot-dependencies:4.1.1@pom
//DEPS org.springframework.boot:spring-boot-starter-webflux
//DEPS org.jetbrains.kotlinx:kotlinx-coroutines-reactor
//DEPS com.nimbusds:nimbus-jose-jwt:10.5
//FILES application.properties

package sbwebfilter

import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
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
import java.text.ParseException
import java.time.Instant

const val ACCESS_TOKEN_HEADER = "X-KKK-AccessToken"
const val ACCESS_TOKEN_ATTRIBUTE = "accessToken"

private val log = LoggerFactory.getLogger("sbwebfilter.UserRouter")

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
            GET("/users") { request ->
                log.info("{} asked for the users", request.accessToken().subject)
                ok().bodyValueAndAwait(userService.getUsers())
            }
            GET("/users/{id}") { request ->
                log.info("{} asked for the user {}", request.accessToken().subject, request.pathVariable("id"))
                userService.getUser(request.pathVariable("id"))
                    ?.let { ok().bodyValueAndAwait(it) }
                    ?: notFound().buildAndAwait()
            }
        }
    }

    registerBean<JwtReader>()

    registerBean("userEndpoints") {
        RouterEndpoints(
            router = bean<RouterFunction<ServerResponse>>("userRouter"),
            messageReaders = bean<ServerCodecConfigurer>().readers,
        )
    }

    // registered before AccessTokenFilter, so the 401 responses are logged too
    registerBean { LoggingFilter(endpoints = bean<RouterEndpoints>("userEndpoints")) }

    registerBean {
        AccessTokenFilter(
            endpoints = bean<RouterEndpoints>("userEndpoints"),
            jwtReader = bean<JwtReader>(),
        )
    }
})

// The endpoints declared by the given router, picked up from the router itself (no hard-coded path patterns).
class RouterEndpoints(
    private val router: RouterFunction<ServerResponse>,
    private val messageReaders: List<HttpMessageReader<*>>,
) {

    suspend fun matches(exchange: ServerWebExchange): Boolean {
        // match on a copy: routing writes path attributes that must not leak into the real exchange
        val probe = ServerRequest.from(ServerRequest.create(exchange, messageReaders)).build()
        return router.route(probe).awaitSingleOrNull() != null
    }
}

// Applies to the given endpoints; other paths pass through unlogged.
class LoggingFilter(private val endpoints: RouterEndpoints) : CoWebFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val routed = endpoints.matches(exchange)

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

// Applies to the given endpoints: answers 401 unless the access token header carries a readable JWT.
class AccessTokenFilter(
    private val endpoints: RouterEndpoints,
    private val jwtReader: JwtReader,
) : CoWebFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        if (!endpoints.matches(exchange)) {
            return chain.filter(exchange)
        }

        val accessToken = exchange.request.headers.getFirst(ACCESS_TOKEN_HEADER)?.let(jwtReader::read)
        if (accessToken == null) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            exchange.response.setComplete().awaitSingleOrNull()
            return
        }

        log.info("access token: {}", accessToken)
        exchange.attributes[ACCESS_TOKEN_ATTRIBUTE] = accessToken
        chain.filter(exchange)
    }
}

// The access token read by AccessTokenFilter, for the handlers of the endpoints the filter is applied on.
fun ServerRequest.accessToken(): AccessToken =
    attributes()[ACCESS_TOKEN_ATTRIBUTE] as? AccessToken
        ?: error("No access token: is AccessTokenFilter applied on ${method()} ${path()}?")

class JwtReader {

    // reads the content of a signed JWT only: the signature is not checked
    fun read(token: String): AccessToken? =
        try {
            val jwt = SignedJWT.parse(token)
            val claims = jwt.jwtClaimsSet

            AccessToken(
                header = jwt.header.toJSONObject(),
                subject = claims.subject,
                issuer = claims.issuer,
                expiration = claims.expirationTime?.toInstant(),
                claims = claims.claims,
            )
        } catch (e: ParseException) {
            null
        }
}

data class AccessToken(
    val header: Map<String, Any?>,
    val subject: String?,
    val issuer: String?,
    val expiration: Instant?,
    val claims: Map<String, Any?>,
)

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
