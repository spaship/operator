package io.websitecd.operator.config;

import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class NamespacesTest {

    @Test
    public void testNamespaces() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/namespaces-test.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);

            assertEquals(3, config.getEnvs().size());
            assertEquals("ns-1", config.getEnvironment("dev").getNamespace());
            assertEquals("ns-2", config.getEnvironment("stage").getNamespace());
            assertEquals("ns-3", config.getEnvironment("prod").getNamespace());
        }
    }

}
