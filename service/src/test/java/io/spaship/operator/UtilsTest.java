package io.spaship.operator;

import io.spaship.operator.config.OperatorConfigUtils;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.controller.WebsiteRepository;
import io.spaship.operator.crd.Website;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static io.spaship.operator.rest.WebhookTestCommon.NAMESPACE;
import static io.spaship.operator.rest.WebhookTestCommon.SIMPLE_WEB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
class UtilsTest {

    @Test
    public void testLabels() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/labels-test.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);

            Website website = WebsiteRepository.createWebsite("test-simple", SIMPLE_WEB, NAMESPACE);
            website.setConfig(config);

            Map<String, String> labels = Utils.defaultLabels("test", website);
            assertEquals("test-simple", labels.get("website"));
            assertEquals("test", labels.get("env"));
            assertEquals("code", labels.get("appcode"));
            assertEquals("label2value", labels.get("label2"));
        }
    }


    @Test
    void testBuildEventPayloadCase1(){
        JsonObject expectedOutcome =  new JsonObject();
        expectedOutcome.put("name","sample").put("ns","operator-test");
        String outcome = Utils.buildEventPayload("name:sample","ns:operator-test");
        assertEquals(expectedOutcome.toString(),outcome);
        assertEquals(expectedOutcome, new JsonObject(outcome));
    }
    @Test
    void testBuildEventPayloadCase2(){
        JsonObject expectedOutcome =  new JsonObject();
        expectedOutcome.put("name","sample").put("attr_1","operator-test");
        String outcome = Utils.buildEventPayload("name:sample","operator-test");
        assertEquals(expectedOutcome.toString(),outcome);
        assertEquals(expectedOutcome, new JsonObject(outcome));
    }
    @Test
    void testBuildEventPayloadCase3(){
        JsonObject expectedOutcome =  new JsonObject();
        expectedOutcome.put("name","sample").put("ns","operator-test");
        String outcome = Utils.buildEventPayload("name:sample","operator-test");
        assertNotEquals(expectedOutcome.toString(),outcome);
        assertNotEquals(expectedOutcome, new JsonObject(outcome));
    }

}