package io.websitecd.operator.config;

import io.websitecd.TestUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MetadataTest {

    private String testedFile = "/metadata-test.yaml";

    @Test
    public void testLabels() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        Map<String, String> metadata = config.getMetadata();
        assertNotNull(metadata);
        assertEquals("value", metadata.get("key"));
    }

}
