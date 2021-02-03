package io.websitecd.operator.webhook.gitlab;

import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.PushEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link GitlabWebHookListener}
 */
class GitlabWebHookListenerTest {

    @Test
    void isRolloutNeeded() {
        String[] yamls = {"website.yaml", "website.yml"};
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(false, true, "website.yaml"), yamls));
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(true, false, "website.yaml"), yamls));
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(false, true, "path/website.yaml"), yamls));
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(true, false, "path/website.yaml"), yamls));

        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(false, true, "website.yml"), yamls));
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(true, false, "website.yml"), yamls));
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(false, true, "path/website.yml"), yamls));
        assertTrue(GitlabWebHookListener.isRolloutNeeded(getEvent(true, false, "path/website.yml"), yamls));

        assertFalse(GitlabWebHookListener.isRolloutNeeded(getEvent(false, true, "app.css"), yamls));
        assertFalse(GitlabWebHookListener.isRolloutNeeded(getEvent(true, false, "app.css"), yamls));
    }

    public PushEvent getEvent(boolean added, boolean modified, String... files) {
        List<EventCommit> commits = new ArrayList<>();
        EventCommit commit = new EventCommit();
        if (added)
            commit.setAdded(Arrays.asList(files));
        if (modified)
            commit.setModified(Arrays.asList(files));
        commits.add(commit);
        PushEvent event = new PushEvent();
        event.setCommits(commits);
        return event;
    }
}