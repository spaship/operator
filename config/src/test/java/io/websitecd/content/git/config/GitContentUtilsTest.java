package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.content.git.config.model.GitComponent;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class GitContentUtilsTest {

    @Test
    public void testLoadConfig() throws IOException {
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/gitconfig-test.yaml");
        ContentConfig config = GitContentUtils.loadYaml(is);
        is.close();

        assertEquals(2, config.getComponents().size());
        assertEquals("_root", config.getComponents().get(0).getDir());
    }

    @Test
    public void createConfig() throws IOException {
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/staticcontent-website-test.yaml");
        WebsiteConfig websiteConfig = OperatorConfigUtils.loadYaml(is);
        is.close();

        String env = "test";

        String rootContext = "/_root_test/";
        ContentConfig testConfig = GitContentUtils.createConfig(env, websiteConfig, rootContext);

        assertEquals(2, testConfig.getComponents().size());
        GitComponent component1 = testConfig.getComponents().get(0);
        assertEquals("test-only-dev", component1.getDir());
        assertEquals("git", component1.getKind());
        assertEquals("giturl1", component1.getSpec().getUrl());
        assertEquals("/subidr", component1.getSpec().getDir());
        assertEquals("special-branch", component1.getSpec().getRef());

        GitComponent component2 = testConfig.getComponents().get(1);
        assertEquals("_root_test/", component2.getDir());
        assertEquals("git", component2.getKind());
        assertEquals("giturl2", component2.getSpec().getUrl());
        assertEquals("/", component2.getSpec().getDir());
        assertEquals(env, component2.getSpec().getRef());

        ContentConfig prodConfig = GitContentUtils.createConfig("prod", websiteConfig, rootContext);
        assertEquals(2, prodConfig.getComponents().size());
        GitComponent prod1 = prodConfig.getComponents().get(0);
        assertEquals("prod-only", prod1.getDir());
        assertEquals("git", prod1.getKind());
        assertEquals("giturl2", prod1.getSpec().getUrl());
        assertEquals("/", prod1.getSpec().getDir());
        assertEquals("prod", prod1.getSpec().getRef());
    }

    @Test
    public void createConfigInvalidEnv() throws IOException {
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/staticcontent-website-test.yaml");
        WebsiteConfig websiteConfig = OperatorConfigUtils.loadYaml(is);
        is.close();

        String env = "invalid";

        String rootContext = "/_root_test/";
        ContentConfig config = GitContentUtils.createConfig(env, websiteConfig, rootContext);

        assertEquals(0, config.getComponents().size());
    }
}
