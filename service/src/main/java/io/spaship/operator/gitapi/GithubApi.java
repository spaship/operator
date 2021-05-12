package io.spaship.operator.gitapi;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GithubApi implements GitApi {

    private static final Logger log = Logger.getLogger(GitlabApi.class);

    @Override
    public Future<JsonObject> commentMergeRequest(String host, String projectId, String mergeRequestId, String accessToken, String body) {
        // TODO: Implement commentMergeRequest in GithubApi
        log.info("Comment Merge Request in Github NOT implemented yet");
        return Future.succeededFuture(new JsonObject());
    }
}
