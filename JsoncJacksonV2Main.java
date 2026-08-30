///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS tools.jackson.core:jackson-databind:3.2.2
//PREVIEW
//FILES organization.jsonc

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.json.JsonReadFeature;

import java.io.InputStream;
import java.util.List;

record Organization(String name, String ceo, List<String> departments) {}

public class JsoncJacksonV2Main {
    public static void main(String[] args) {
        JsonMapper mapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .build();
        InputStream is = Organization.class.getResourceAsStream("/organization.jsonc");
        var org = mapper.readValue(is, Organization.class);
        System.out.println("org = " + org);
    }
}
