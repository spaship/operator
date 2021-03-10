package io.websitecd.operator;

import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static io.websitecd.operator.rest.WebhookTestCommon.NAMESPACE;
import static io.websitecd.operator.rest.WebhookTestCommon.SIMPLE_WEB;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}