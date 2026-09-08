///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.8
//DEPS org.springframework.boot:spring-boot-starter-data-cassandra:4.0.8
//DEPS org.slf4j:slf4j-api:2.0.17
//FILES application.properties

package sbc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@SpringBootApplication
public class SpringBootCassandraMain {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCassandraMain.class, args);
    }

    @Bean
    OAuth2ClientConfigRepository repository(CassandraOperations operations) {
        return new CassandraRepositoryFactory(operations).getRepository(OAuth2ClientConfigRepository.class);
    }

    @Table("oauth2clientconfig")
    public static class OAuth2ClientConfigEntity {

        @PrimaryKey
        private OAuth2ClientConfigKey key;

        @Column("version")
        private String version;

        @Column("response_mode")
        private String responseMode;

        @Column("request_uri_method")
        private String requestUriMethod;

        @Column("redirect_uri")
        private String redirectUri;

        @Column("verifier_info")
        private String verifierInfo;

        public OAuth2ClientConfigKey getKey() {
            return key;
        }

        public void setKey(OAuth2ClientConfigKey key) {
            this.key = key;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getResponseMode() {
            return responseMode;
        }

        public void setResponseMode(String responseMode) {
            this.responseMode = responseMode;
        }

        public String getRequestUriMethod() {
            return requestUriMethod;
        }

        public void setRequestUriMethod(String requestUriMethod) {
            this.requestUriMethod = requestUriMethod;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getVerifierInfo() {
            return verifierInfo;
        }

        public void setVerifierInfo(String verifierInfo) {
            this.verifierInfo = verifierInfo;
        }

        OAuth2ClientConfig toDomain() {
            OAuth2ClientConfig.OAuth2 oauth2 = new OAuth2ClientConfig.OAuth2(
                    key.getClientId(),
                    key.getBusinessPurpose(),
                    responseMode != null ? OAuth2ClientConfig.ResponseMode.valueOf(responseMode) : null,
                    requestUriMethod != null ? OAuth2ClientConfig.RequestUriMethod.valueOf(requestUriMethod) : null,
                    redirectUri,
                    verifierInfo);
            return new OAuth2ClientConfig(oauth2, version);
        }

        static OAuth2ClientConfigEntity from(OAuth2ClientConfig config) {
            OAuth2ClientConfigEntity entity = new OAuth2ClientConfigEntity();
            entity.setKey(new OAuth2ClientConfigKey(config.oauth2().clientId(), config.oauth2().businessPurpose()));
            entity.setVersion(config.version());
            entity.setResponseMode(config.oauth2().responseMode() != null ? config.oauth2().responseMode().name() : null);
            entity.setRequestUriMethod(config.oauth2().requestUriMethod() != null ? config.oauth2().requestUriMethod().name() : null);
            entity.setRedirectUri(config.oauth2().redirectUri());
            entity.setVerifierInfo(config.oauth2().verifierInfo());
            return entity;
        }
    }

    @PrimaryKeyClass
    public static class OAuth2ClientConfigKey {

        @PrimaryKeyColumn(name = "client_id", type = PrimaryKeyType.PARTITIONED)
        private String clientId;

        @PrimaryKeyColumn(name = "business_purpose", type = PrimaryKeyType.CLUSTERED)
        private String businessPurpose;

        public OAuth2ClientConfigKey() {
        }

        public OAuth2ClientConfigKey(String clientId, String businessPurpose) {
            this.clientId = clientId;
            this.businessPurpose = businessPurpose;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getBusinessPurpose() {
            return businessPurpose;
        }

        public void setBusinessPurpose(String businessPurpose) {
            this.businessPurpose = businessPurpose;
        }
    }

    public interface OAuth2ClientConfigRepository extends CassandraRepository<OAuth2ClientConfigEntity, OAuth2ClientConfigKey> {

        Optional<OAuth2ClientConfigEntity> findByKeyClientIdAndKeyBusinessPurpose(String clientId, String businessPurpose);

        default OAuth2ClientConfigEntity doSave(OAuth2ClientConfigEntity entity) {
            try {
                return save(entity);
            } catch (RuntimeException e) {
                throw new OAuth2ClientConfigPersistenceException(e.getMessage(), e);
            }
        }
    }

    public static class OAuth2ClientConfigPersistenceException extends RuntimeException {
        OAuth2ClientConfigPersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public record OAuth2ClientConfig(OAuth2 oauth2, String version) {
        public record OAuth2(
                String clientId,
                String businessPurpose,
                ResponseMode responseMode,
                RequestUriMethod requestUriMethod,
                String redirectUri,
                String verifierInfo) {
        }

        public enum ResponseMode {
            direct_post
        }

        public enum RequestUriMethod {
            get,
            post
        }
    }

    @RestController
    static class OAuth2ClientConfigController {

        private final OAuth2ClientConfigRepository repository;

        OAuth2ClientConfigController(OAuth2ClientConfigRepository repository) {
            this.repository = repository;
        }

        @GetMapping("/oauth2-config")
        List<OAuth2ClientConfig> getConfigs() {
            return repository.findAll().stream()
                    .map(OAuth2ClientConfigEntity::toDomain)
                    .collect(Collectors.toList());
        }

        @GetMapping(value = "/oauth2-config", params = {"clientId", "businessPurpose"})
        ResponseEntity<OAuth2ClientConfig> findConfig(@RequestParam String clientId, @RequestParam String businessPurpose) {
            return repository.findByKeyClientIdAndKeyBusinessPurpose(clientId, businessPurpose)
                    .map(entity -> ResponseEntity.ok(entity.toDomain()))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @PostMapping("/oauth2-config")
        OAuth2ClientConfig createConfig(@RequestBody OAuth2ClientConfig config) {
            OAuth2ClientConfigEntity saved = repository.doSave(OAuth2ClientConfigEntity.from(config));
            return saved.toDomain();
        }
    }

    @RestControllerAdvice
    static class OAuth2ClientConfigControllerAdvice {

        @ExceptionHandler(OAuth2ClientConfigPersistenceException.class)
        ResponseEntity<Map<String, String>> handle(OAuth2ClientConfigPersistenceException ex) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("error", "persistence");
            body.put("message", ex.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }
}
