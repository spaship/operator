package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class GitConfigTest {

    @Test
    public void testLoadGitConfig() throws IOException {
        // Just try to load git config
        try (InputStream is = GitContentUtils.class.getResourceAsStream("/gitconfig-test.yaml")) {
            ContentConfig config = GitContentUtils.loadYaml(is);
            assertEquals(3, config.getComponents().size());
        }
    }

}
