package io.websitecd.operator.config;

import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MetadataTest {

    @Test
    public void testLabels() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/metadata-test.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);
            Map<String, String> metadata = config.getMetadata();
            assertNotNull(metadata);
            assertEquals("websitename", config.getWebsiteName());
        }
    }

}
