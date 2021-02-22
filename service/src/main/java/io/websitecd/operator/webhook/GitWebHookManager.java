package io.websitecd.operator.webhook;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.crd.Website;

import java.util.List;

public interface GitWebHookManager {
    public boolean canHandleRequest(HttpServerRequest request);

    public void validateRequest(HttpServerRequest request, JsonObject data) throws Exception;

    String getGitUrl(JsonObject postData);

    String getRef(JsonObject postData);

    public List<Website> getAuthorizedWebsites(HttpServerRequest request, JsonObject postData);
}
