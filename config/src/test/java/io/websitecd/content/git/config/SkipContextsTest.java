package io.websitecd.content.git.config;

import io.websitecd.TestUtils;
import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SkipContextsTest {

    String testedFile = "/skipContexts-test.yaml";

    @Test
    public void testSkipContextsTestEnv() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        ContentConfig gitConfig = GitContentUtils.createConfig("test", config, null);
        assertEquals(2, gitConfig.getComponents().size());
        assertEquals("both", gitConfig.getComponents().get(0).getDir());
        assertEquals("test", gitConfig.getComponents().get(1).getDir());
    }

    @Test
    public void testSkipContextsProdEnv() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        ContentConfig gitConfig = GitContentUtils.createConfig("prod", config, null);
        assertEquals(2, gitConfig.getComponents().size());
        assertEquals("both", gitConfig.getComponents().get(0).getDir());
        assertEquals("prod", gitConfig.getComponents().get(1).getDir());
    }
}
