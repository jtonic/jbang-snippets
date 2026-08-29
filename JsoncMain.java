///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS tools.jackson.core:jackson-databind:3.2.2
//PREVIEW
//FILES organization.jsonc

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.json.JsonReadFeature;
import java.util.List;

record Organization(String name, String ceo, List<String> departments) {}

void main() {
    JsonMapper mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();
    InputStream is = Organization.class.getResourceAsStream("/organization.jsonc");
    var org = mapper.readValue(is, Organization.class);
    IO.println("org = " + org);
}
