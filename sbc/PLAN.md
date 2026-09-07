# PLAN

This is going to create a SpringBootCassandraMain application (Java file) for jbang,
along with the companion SpringBootCassandraTCTest (Java file) for jbang 

1. [x] Create a docker-compose.yml
    - [x] with a single service for Cassandra
    - [x] and having a schema.cql file to be automatically run when the C* docker container is created, having:
      a) create a single schema (or keyspace) named `verifier`
      b) with a single table created, named oauth2clientconfig. with the following structure - two columns (client_id <String> and businessPurpose <String>),
      b) and populate this table with two raws.
   Validate the success of those steps using the docker commands, and cql command run inside the running docker
   to show the raws in the created table
2. [x] Considering [this example](../sb/SbAppMain.java) as an example:
   - [x] Use springboot 4.0 (latest) as spring boot dependencies,
   - [x] Use spring data & spring data cassandra as dependencies,
   - [x] Create a new Rest Controller named OAuth2ClientConfigController
    a) with only one endpoint (/oauth2-config) that delegate to:
    b) a Spring Data Cassandra Repository # findAll (returning a list of Cassandra entities)
    c) convert the list of entities to DTOs (java records) 
    d) and the list of DTOs are converted automatically by Spring Web (MVC) to JSON
    e) any needed information (configuration) related to Cassandra goes in a new application.properties in the [sbc](.) folder
   - [x] Chek a successful implementation of this step is by successfully started the application with `jbang` and use `curl` and `jq` 
   to see if the data from cassandra are returned to the cURL invoker.
3. [x] Next it is about adapting the C* keyspace table oauth2clientconfig and all that is related considering the following structure (as java class)
   that comes via a new endpoint (it is the DTO that maps to the domain data model), to persist to C* (via mapTo Cassandra @Table data entity java class).
   ```java
   public record OAuth2ClientConfig(  
        OAuth2 oauth2,  
        String version  
   ) {
        public record OAuth2(  
                String clientId,  
                String businessPurpose,  
                ResponseMode responseMode,  
                RequestUriMethod requestUriMethod,  
                String redirectUri,  
                String verifierInfo  
        ) {}  
        public enum ResponseMode {  
            direct_post  
        }  
        public enum RequestUriMethod {  
            get,  
            post  
        }  
   }
   ```
   - [x] This requires a compound primary key with `client_id` as partition key and `business_purpose` as clustering column: `PRIMARY KEY ((client_id), business_purpose)`. Note naming convention: CQL columns use snake_case, Java fields use camelCase — the `@Column("snake_case")` annotation on the `@Table` entity handles the mapping (e.g. `client_id` ↔ `clientId`, `business_purpose` ↔ `businessPurpose`, `response_mode` ↔ `responseMode`, etc.),
   - [x] This requires also a docker compose down with the options to delete the container and any volumes created,
- [x] This requires updating the @Table Cassandra entity class to flatten the nested DTO fields into a single `OAuth2ClientConfigEntity` with `@Column` annotations mapping camelCase Java fields to snake_case CQL columns,
- [x] This requires the update of the cql file to be used for the automatic execution when the cassandra new container is started via docker compose up command,
- [x] This requires the adaptation of the map between the DTO and the @Table Cassandra data entity,
- [x] Adapt the [test.http](sbc/test.http) to add a new http command for the insertion,
- [x] This also requires the update of the check for correct implementation,
- [x] Check again the current implementation with the new endpoint (insert a new raw in the oauth2clientconfig), cURL and jq
