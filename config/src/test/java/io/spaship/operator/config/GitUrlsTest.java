package io.spaship.operator.config;

import io.spaship.TestUtils;
import io.spaship.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GitUrlsTest {

    private String testedFile = "/giturls-test.yaml";

    @Test
    public void testGitUrls() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        assertNull(config.getComponents().get(0).getSpec().getUrl());
        assertEquals("defined-git-url", config.getComponents().get(1).getSpec().getUrl());

        String defaultGitUrl = "default-git-url";
        int applied = OperatorConfigUtils.applyDefaultGirUrl(config, defaultGitUrl);
        assertEquals("defined-git-url", config.getComponents().get(1).getSpec().getUrl());
        assertEquals(defaultGitUrl, config.getComponents().get(0).getSpec().getUrl());
        assertEquals(1, applied);
    }

}
