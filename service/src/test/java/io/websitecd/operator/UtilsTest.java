package io.websitecd.operator;

import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    public void testLabels() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/labels-test.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);

            Map<String, String> labels = Utils.defaultLabels("test", config);
            assertEquals("test", labels.get("env"));
            assertEquals("code", labels.get("appcode"));
            assertEquals("label2value", labels.get("label2"));
        }
    }

}