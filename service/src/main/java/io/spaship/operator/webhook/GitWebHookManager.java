package io.spaship.operator.webhook;

import io.spaship.operator.crd.Website;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

import static io.spaship.operator.webhook.model.UpdatedWebsite.*;

public interface GitWebHookManager {

    public static enum MergeStatus {
        OPEN, UPDATE, CLOSE;

        public String toResponseStatus() {
            switch (this) {
                case OPEN:
                    return STATUS_PREVIEW_CREATING;
                case UPDATE:
                    return STATUS_PREVIEW_UPDATING;
                case CLOSE:
                    return STATUS_PREVIEW_DELETING;
            }
            return "unknown";
        }
    }

    boolean canHandleRequest(HttpServerRequest request);

    public String getEventHeader(HttpServerRequest request);

    boolean isMergeRequest(HttpServerRequest request);

    MergeStatus getMergeStatus(JsonObject data);

    void validateRequest(HttpServerRequest request, JsonObject data) throws Exception;

    String getGitUrl(HttpServerRequest request, JsonObject postData);

    String getRef(JsonObject postData);

    String getPreviewGitUrl(JsonObject postData);

    String getPreviewRef(JsonObject postData);

    String getPreviewId(JsonObject postData);

    List<Website> getAuthorizedWebsites(HttpServerRequest request, JsonObject postData);

    // GitHub issue 65
    JsonObject extractRepositoryInformation(JsonObject data);

    enum RepoType {GITHUB,GITLAB}
    // GitHub issue 65
}
