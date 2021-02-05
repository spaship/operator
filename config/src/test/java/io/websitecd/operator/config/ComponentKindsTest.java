package io.websitecd.operator.config;

import io.websitecd.TestUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ComponentKindsTest {

    String testedFile = "/git-service-test.yaml";


    @Test
    public void testComponentKinds() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        assertEquals(3, config.getComponents().size());
        assertEquals("/test1", config.getComponents().get(0).getContext());
        assertEquals(true, config.getComponents().get(0).isKindGit());
        assertEquals("/test2", config.getComponents().get(1).getContext());

        ComponentConfig config3 = config.getComponents().get(2);
        assertEquals("/test3", config3.getContext());
        assertEquals("service", config3.getKind());
        assertEquals(true, config3.isKindService());
        assertEquals(false, config3.isKindGit());
        assertEquals("api", config3.getSpec().getServiceName());
        assertEquals(Integer.valueOf(80), config3.getSpec().getTargetPort());
    }

}
