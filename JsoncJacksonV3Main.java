///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS com.fasterxml.jackson.core:jackson-databind:2.22.1
//DEPS com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.22.1
//PREVIEW
//FILES organization.jsonc

package jackson.v3;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.InputStream;
import java.util.*;

import static com.fasterxml.jackson.core.JsonParser.Feature;

record Organization(String name, String ceo, List<String> departments) {}

public class JsoncJacksonV3Main {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = createDefaultMapper();
        InputStream is = Organization.class.getResourceAsStream("/organization.jsonc");
        var org = mapper.readValue(is, Organization.class);
        System.out.println("org = " + org);
    }

    public static ObjectMapper createDefaultMapper() {
        return new ObjectMapper()
                .registerModule(new Jdk8Module())
                .configure(Feature.ALLOW_COMMENTS, true)
                .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
