package io.spaship.operator.webhook.gitlab;

import io.spaship.operator.webhook.GitWebHookManager;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GitlabWebHookManagerTest {

    @Test
    void getMergeStatus() {
        GitlabWebHookManager tested = new GitlabWebHookManager();
        Assertions.assertNull(tested.getMergeStatus(new JsonObject().put("object_attributes", new JsonObject().put("state", "").put("action", ""))));

        Assertions.assertEquals(GitWebHookManager.MergeStatus.OPEN,
                tested.getMergeStatus(new JsonObject().put("object_attributes", new JsonObject().put("state", "opened").put("action", "open"))));
        Assertions.assertEquals(GitWebHookManager.MergeStatus.UPDATE,
                tested.getMergeStatus(new JsonObject().put("object_attributes", new JsonObject().put("state", "opened").put("action", "update"))));
        Assertions.assertEquals(GitWebHookManager.MergeStatus.CLOSE,
                tested.getMergeStatus(new JsonObject().put("object_attributes", new JsonObject().put("state", "closed").put("action", "close"))));
    }
}