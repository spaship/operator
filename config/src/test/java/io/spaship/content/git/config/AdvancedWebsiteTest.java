package io.spaship.content.git.config;

import io.spaship.TestUtils;
import io.spaship.content.git.config.model.ContentConfig;
import io.spaship.content.git.config.model.GitComponent;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AdvancedWebsiteTest {

    String rootContext = "/_root_test";

    @Test
    public void testBranchesTestEnv() throws IOException {
        WebsiteConfig websiteConfig = TestUtils.loadConfig("/advanced-website-test.yaml");

        ContentConfig testConfig = GitContentUtils.createConfig("test", websiteConfig, rootContext);

        assertEquals(3, testConfig.getComponents().size());
        GitComponent component1 = testConfig.getComponents().get(0);
        assertEquals("test", component1.getDir());
        assertEquals(ComponentConfig.KIND_GIT, component1.getKind());
        assertEquals("giturl1", component1.getSpec().getUrl());
        assertEquals("special-branch", component1.getSpec().getRef());

        GitComponent component2 = testConfig.getComponents().get(1);
        assertEquals("theme", component2.getDir());
        assertEquals(ComponentConfig.KIND_GIT, component2.getKind());
        assertEquals("giturl2", component2.getSpec().getUrl());
        assertEquals("2.0.0", component2.getSpec().getRef());

        GitComponent component3 = testConfig.getComponents().get(2);
        assertEquals("_root_test", component3.getDir());
        assertEquals(ComponentConfig.KIND_GIT, component3.getKind());
        assertEquals("giturl3", component3.getSpec().getUrl());
        assertEquals("test", component3.getSpec().getRef());
    }

    @Test
    public void testBranchesProdEnv() throws IOException {
        WebsiteConfig websiteConfig = TestUtils.loadConfig("/advanced-website-test.yaml");

        ContentConfig prodConfig = GitContentUtils.createConfig("prod", websiteConfig, rootContext);
        assertEquals(3, prodConfig.getComponents().size());
        GitComponent prod1 = prodConfig.getComponents().get(1);
        assertEquals("theme", prod1.getDir());
        assertEquals(ComponentConfig.KIND_GIT, prod1.getKind());
        assertEquals("giturl2", prod1.getSpec().getUrl());
        assertEquals("1.0.0", prod1.getSpec().getRef());
    }

    @Test
    public void testInvalidEnv() throws IOException {
        WebsiteConfig websiteConfig = TestUtils.loadConfig("/advanced-website-test.yaml");

        String env = "invalid";

        ContentConfig config = GitContentUtils.createConfig(env, websiteConfig, rootContext);

        assertEquals(0, config.getComponents().size());
    }
}
