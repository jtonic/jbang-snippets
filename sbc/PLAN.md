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
3. [ ] Next step that will be filled in when the previous one is successful 
