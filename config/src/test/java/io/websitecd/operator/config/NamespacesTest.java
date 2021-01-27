package io.websitecd.operator.config;

import io.websitecd.TestUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class NamespacesTest {

    private String testedFile = "/namespaces-test.yaml";

    @Test
    public void testNamespaces() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        assertEquals(3, config.getEnvs().size());
        assertEquals("ns-1", config.getEnvironment("dev").getNamespace());
        assertEquals("ns-2", config.getEnvironment("stage").getNamespace());
        assertEquals("ns-3", config.getEnvironment("prod").getNamespace());
    }

}
