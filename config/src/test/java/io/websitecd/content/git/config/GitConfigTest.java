package io.websitecd.content.git.config;

import io.websitecd.content.git.config.model.ContentConfig;
import io.websitecd.operator.config.OperatorConfigUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class GitConfigTest {

    @Test
    public void testLoadGitConfig() throws IOException {
        // Just try to load git config
        InputStream is = OperatorConfigUtils.class.getResourceAsStream("/gitconfig-test.yaml");
        ContentConfig config = GitContentUtils.loadYaml(is);
        is.close();

        assertEquals(3, config.getComponents().size());
    }

}
