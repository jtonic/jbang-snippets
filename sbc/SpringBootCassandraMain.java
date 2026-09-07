///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.8
//DEPS org.springframework.boot:spring-boot-starter-data-cassandra:4.0.8
//DEPS org.slf4j:slf4j-api:2.0.17
//FILES application.properties

package sbc;

import java.util.List;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

        OAuth2ClientConfigEntity toEntity() {
            OAuth2ClientConfigEntity entity = new OAuth2ClientConfigEntity();
            entity.setKey(new OAuth2ClientConfigKey(oauth2.clientId(), oauth2.businessPurpose()));
            entity.setVersion(version);
            entity.setResponseMode(oauth2.responseMode() != null ? oauth2.responseMode().name() : null);
            entity.setRequestUriMethod(oauth2.requestUriMethod() != null ? oauth2.requestUriMethod().name() : null);
            entity.setRedirectUri(oauth2.redirectUri());
            entity.setVerifierInfo(oauth2.verifierInfo());
            return entity;
        }

        static OAuth2ClientConfig from(OAuth2ClientConfigEntity entity) {
            OAuth2 oauth2 = new OAuth2(
                    entity.getKey().getClientId(),
                    entity.getKey().getBusinessPurpose(),
                    entity.getResponseMode() != null ? ResponseMode.valueOf(entity.getResponseMode()) : null,
                    entity.getRequestUriMethod() != null ? RequestUriMethod.valueOf(entity.getRequestUriMethod()) : null,
                    entity.getRedirectUri(),
                    entity.getVerifierInfo());
            return new OAuth2ClientConfig(oauth2, entity.getVersion());
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
                    .map(OAuth2ClientConfig::from)
                    .collect(Collectors.toList());
        }

        @PostMapping("/oauth2-config")
        OAuth2ClientConfig createConfig(@RequestBody OAuth2ClientConfig config) {
            return OAuth2ClientConfig.from(repository.save(config.toEntity()));
        }
    }
}
