package io.websitecd.operator.config;

import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OperatorConfigUtilsTest {

    @Test
    public void testWebsite() throws IOException {
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/website-test.yaml");
        WebsiteConfig config = OperatorConfigUtils.loadYaml(is);
        is.close();
        assertEquals("websitename", config.getMetadata().get("name"));


        assertEquals(3, config.getEnvs().size());
        assertEquals("ns-1", config.getEnvironment("dev").getNamespace());

        assertEquals(3, config.getComponents().size());
        assertEquals("/test1", config.getComponents().get(0).getContext());
        assertEquals(true, config.getComponents().get(0).isKindGit());
        assertEquals("/test2", config.getComponents().get(1).getContext());

        ComponentConfig config3 = config.getComponents().get(2);
        assertEquals("/test3", config3.getContext());
        assertEquals("service", config3.getKind());
        assertEquals(true, config3.isKindService());
        assertEquals("api", config3.getSpec().getServiceName());
        assertEquals("80", config3.getSpec().getTargetPort());
    }

    @Test
    public void getRootComponentSubdir() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/website-test.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);
            assertNull(OperatorConfigUtils.getRootComponentSubdir(config));
        }

        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/staticcontent-website-test.yaml")) {
            WebsiteConfig config2 = OperatorConfigUtils.loadYaml(is);
            assertEquals("subdir", OperatorConfigUtils.getRootComponentSubdir(config2));
        }

    }
}
