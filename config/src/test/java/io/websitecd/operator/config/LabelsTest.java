package io.websitecd.operator.config;

import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LabelsTest {

    @Test
    public void testLabels() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/labels-test.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);
            Map<String, String> labels = config.getLabels();
            assertNotNull(labels);
            assertEquals("code", labels.get("appcode"));
            assertEquals("label2value", labels.get("label2"));
        }
    }

}
