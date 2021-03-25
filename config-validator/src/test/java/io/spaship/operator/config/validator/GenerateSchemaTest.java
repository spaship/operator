package io.spaship.operator.config.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import io.spaship.operator.config.model.WebsiteConfig;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class GenerateSchemaTest {

    private static final Logger log = Logger.getLogger(GenerateSchemaTest.class);

    @Test
    void testGenerateWebsiteConfigSchema() throws IOException {
        JsonNode jsonSchema = generateSchema();

        String schemaPath = System.getProperty("schema.path", "target/websiteconfig-schema.json");

        File configSchema = new File(schemaPath);
        log.info("Generating schema to file. path=" + configSchema.getAbsolutePath());
        FileWriter json = new FileWriter(configSchema);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(json, jsonSchema);
    }

    public static JsonNode generateSchema() {
        ObjectMapper mapper = new ObjectMapper();

        JsonSchemaConfig config = JsonSchemaConfig.vanillaJsonSchemaDraft4().withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);
        JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper, config);

        return generator.generateJsonSchema(WebsiteConfig.class);
    }
}