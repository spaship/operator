package io.websitecd.operator.webhook.gitlab;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.IOUtils;
import org.gitlab4j.api.webhook.Event;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link GitlabWebHookManager}
 */
@QuarkusTest
class GitlabWebHookManagerTest {

    @Inject
    GitlabWebHookManager manager;

    @Test
    void isRolloutNeeded() throws IOException {
        String yaml;
        try (InputStream is = GitlabWebHookManagerTest.class.getResourceAsStream("/gitlab-push.json")) {
            yaml = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        Event event = manager.unmarshal(yaml);
        assertFalse(manager.isRolloutNeeded(event));

        try (InputStream is = GitlabWebHookManagerTest.class.getResourceAsStream("/gitlab-push-website-changed.json")) {
            yaml = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        Event event2 = manager.unmarshal(yaml);
        assertTrue(manager.isRolloutNeeded(event2));
    }
}