package io.spaship.operator.gitapi;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface GitApi {

    public Future<JsonObject> commentMergeRequest(String host, String projectId, String mergeRequestId, String accessToken, String body);
}
