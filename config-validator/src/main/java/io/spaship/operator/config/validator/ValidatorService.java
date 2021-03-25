package io.spaship.operator.config.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@ApplicationScoped
public class ValidatorService {

    JsonSchema schema;

    void onStart(@Observes StartupEvent ev) {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).objectMapper(mapper).build();
        schema = factory.getSchema(ValidatorMain.class.getClassLoader().getResourceAsStream("websiteconfig-schema.json"));
    }

    public Set<ValidationMessage> validate(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode jsonNode = mapper.readTree(is);
        return schema.validate(jsonNode);
    }
}
