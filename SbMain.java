///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter-web:4.1.1
//FILES application.properties

package sb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SbMain {

    public static void main(String[] args) {
        SpringApplication.run(SbMain.class, args);
    }

    @GetMapping("/")
    String hello() {
        return "Hello from Spring Boot on JBang!";
    }
}
