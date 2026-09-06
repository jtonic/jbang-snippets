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
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactory;
import org.springframework.web.bind.annotation.GetMapping;
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
    public static class OAuth2ClientConfig {

        @PrimaryKey("client_id")
        private String clientId;

        @Column("businesspurpose")
        private String businessPurpose;

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

    public interface OAuth2ClientConfigRepository extends CassandraRepository<OAuth2ClientConfig, String> {
    }

    record OAuth2ClientConfigDTO(String clientId, String businessPurpose) {
        static OAuth2ClientConfigDTO from(OAuth2ClientConfig entity) {
            return new OAuth2ClientConfigDTO(entity.getClientId(), entity.getBusinessPurpose());
        }
    }

    @RestController
    static class OAuth2ClientConfigController {

        private final OAuth2ClientConfigRepository repository;

        OAuth2ClientConfigController(OAuth2ClientConfigRepository repository) {
            this.repository = repository;
        }

        @GetMapping("/oauth2-config")
        List<OAuth2ClientConfigDTO> getConfigs() {
            return repository.findAll().stream()
                    .map(OAuth2ClientConfigDTO::from)
                    .collect(Collectors.toList());
        }
    }
}
