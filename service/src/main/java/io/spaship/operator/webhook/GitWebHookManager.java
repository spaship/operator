package io.spaship.operator.webhook;

import io.spaship.operator.crd.Website;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface GitWebHookManager {
    boolean canHandleRequest(HttpServerRequest request);

    void validateRequest(HttpServerRequest request, JsonObject data) throws Exception;

    String getGitUrl(JsonObject postData);

    String getRef(JsonObject postData);

    List<Website> getAuthorizedWebsites(HttpServerRequest request, JsonObject postData);
}
