package io.websitecd.operator.config;

import io.websitecd.TestUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LabelsTest {

    private String testedFile = "/labels-test.yaml";

    @Test
    public void testLabels() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        Map<String, String> labels = config.getLabels();
        assertNotNull(labels);
        assertEquals("code", labels.get("appcode"));
        assertEquals("label2value", labels.get("label2"));
    }

}
