package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.content.git.config.model.GitComponent;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class AdvancedWebsiteTest {

    @Test
    public void testBranches() throws IOException {
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/advanced-website-test.yaml");
        WebsiteConfig websiteConfig = OperatorConfigUtils.loadYaml(is);
        is.close();

        String env = "test";

        String rootContext = "/_root_test";
        ContentConfig testConfig = GitContentUtils.createConfig(env, websiteConfig, rootContext);

        assertEquals(3, testConfig.getComponents().size());
        GitComponent component1 = testConfig.getComponents().get(0);
        assertEquals("test", component1.getDir());
        assertEquals("git", component1.getKind());
        assertEquals("giturl1", component1.getSpec().getUrl());
        assertEquals("special-branch", component1.getSpec().getRef());

        GitComponent component2 = testConfig.getComponents().get(1);
        assertEquals("theme", component2.getDir());
        assertEquals("git", component2.getKind());
        assertEquals("giturl2", component2.getSpec().getUrl());
        assertEquals("2.0.0", component2.getSpec().getRef());

        GitComponent component3 = testConfig.getComponents().get(2);
        assertEquals("_root_test", component3.getDir());
        assertEquals("git", component3.getKind());
        assertEquals("giturl3", component3.getSpec().getUrl());
        assertEquals("test", component3.getSpec().getRef());


        ContentConfig prodConfig = GitContentUtils.createConfig("prod", websiteConfig, rootContext);
        assertEquals(2, prodConfig.getComponents().size());
        GitComponent prod1 = prodConfig.getComponents().get(0);
        assertEquals("theme", prod1.getDir());
        assertEquals("git", prod1.getKind());
        assertEquals("giturl2", prod1.getSpec().getUrl());
        assertEquals("1.0.0", prod1.getSpec().getRef());
    }

    @Test
    public void testInvalidEnv() throws IOException {
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/advanced-website-test.yaml");
        WebsiteConfig websiteConfig = OperatorConfigUtils.loadYaml(is);
        is.close();

        String env = "invalid";

        String rootContext = "/_root_test/";
        ContentConfig config = GitContentUtils.createConfig(env, websiteConfig, rootContext);

        assertEquals(0, config.getComponents().size());
    }
}
