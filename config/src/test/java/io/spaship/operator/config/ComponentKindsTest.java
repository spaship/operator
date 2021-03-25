package io.spaship.operator.config;

import io.spaship.TestUtils;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;
import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentServiceMatcher;
import static org.junit.Assert.assertEquals;

public class ComponentKindsTest {

    String testedFile = "/git-service-test.yaml";
    
    @Test
    public void testComponentKinds() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        assertEquals(3, config.getComponents().size());
        assertEquals("/test1", config.getComponents().get(0).getContext());
        assertEquals(true, ComponentGitMatcher.test(config.getComponents().get(0)));
        assertEquals("/test2", config.getComponents().get(1).getContext());

        ComponentConfig config3 = config.getComponents().get(2);
        assertEquals("/test3", config3.getContext());
        assertEquals("service", config3.getKind());
        assertEquals(true, ComponentServiceMatcher.test(config3));
        assertEquals(false, ComponentGitMatcher.test(config3));
        assertEquals("api", config3.getSpec().getServiceName());
        assertEquals(Integer.valueOf(80), config3.getSpec().getTargetPort());
    }

}
