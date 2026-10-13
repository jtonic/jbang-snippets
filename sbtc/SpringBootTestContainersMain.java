///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter-web:4.1.1
//DEPS org.springframework.boot:spring-boot-starter-restclient:4.1.1
//DEPS org.springframework.boot:spring-boot-starter-data-cassandra:4.1.1
//DEPS org.slf4j:slf4j-api:2.0.17
//FILES application.properties

package sbtc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SpringBootTestContainersMain {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootTestContainersMain.class, args);
    }

    @Bean
    RestClient sbcRestClient(RestClient.Builder builder, @Value("${sbc.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    UserRepository userRepository(CassandraOperations operations) {
        return new CassandraRepositoryFactory(operations).getRepository(UserRepository.class);
    }

    @Component
    static class OAuth2ClientConfigClientService {

        private final RestClient sbcRestClient;
        private final AsyncTaskExecutor executor;

        OAuth2ClientConfigClientService(RestClient sbcRestClient,
                @Qualifier("applicationTaskExecutor") AsyncTaskExecutor executor) {
            this.sbcRestClient = sbcRestClient;
            this.executor = executor;
        }

        CompletableFuture<String> fetchConfigs() {
            return executor.submitCompletable(() -> sbcRestClient.get()
                    .uri("/oauth2-config")
                    .retrieve()
                    .body(String.class));
        }
    }

    @RestController
    static class OAuth2ClientConfigClientController {

        private final OAuth2ClientConfigClientService service;

        OAuth2ClientConfigClientController(OAuth2ClientConfigClientService service) {
            this.service = service;
        }

        @GetMapping(value = "/client/oauth2-config", produces = MediaType.APPLICATION_JSON_VALUE)
        CompletableFuture<String> getConfigs() {
            return service.fetchConfigs();
        }
    }

    @Table("users")
    public static class UserEntity {

        @PrimaryKey("ssn")
        private String ssn;

        @Column("first_name")
        private String firstName;

        @Column("last_name")
        private String lastName;

        @Column("email")
        private String email;

        public String getSsn() {
            return ssn;
        }

        public void setSsn(String ssn) {
            this.ssn = ssn;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        User toDomain() {
            return new User(ssn, firstName, lastName, email);
        }

        static UserEntity from(User user) {
            UserEntity entity = new UserEntity();
            entity.setSsn(user.ssn());
            entity.setFirstName(user.firstName());
            entity.setLastName(user.lastName());
            entity.setEmail(user.email());
            return entity;
        }
    }

    public interface UserRepository extends CassandraRepository<UserEntity, String> {

        default UserEntity doSave(UserEntity entity) {
            try {
                return save(entity);
            } catch (RuntimeException e) {
                throw new UserPersistenceException(e.getMessage(), e);
            }
        }
    }

    public static class UserPersistenceException extends RuntimeException {
        UserPersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public record User(String ssn, String firstName, String lastName, String email) {
    }

    @RestController
    static class UserController {

        private final UserRepository repository;

        UserController(UserRepository repository) {
            this.repository = repository;
        }

        @GetMapping("/users")
        List<User> getUsers() {
            return repository.findAll().stream()
                    .map(UserEntity::toDomain)
                    .toList();
        }

        @GetMapping("/users/{ssn}")
        ResponseEntity<User> getUser(@PathVariable String ssn) {
            return repository.findById(ssn)
                    .map(entity -> ResponseEntity.ok(entity.toDomain()))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @PostMapping("/users")
        User addUser(@RequestBody User user) {
            UserEntity saved = repository.doSave(UserEntity.from(user));
            return saved.toDomain();
        }
    }

    @RestControllerAdvice
    static class UserControllerAdvice {

        @ExceptionHandler(UserPersistenceException.class)
        ResponseEntity<Map<String, String>> handle(UserPersistenceException ex) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "persistence");
            body.put("message", ex.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }
}
