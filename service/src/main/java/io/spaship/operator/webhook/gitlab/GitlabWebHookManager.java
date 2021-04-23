package io.spaship.operator.webhook.gitlab;

import io.spaship.operator.controller.WebsiteRepository;
import io.spaship.operator.crd.Website;
import io.spaship.operator.rest.WebHookResource;
import io.spaship.operator.webhook.GitWebHookManager;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import java.util.List;

@ApplicationScoped
public class GitlabWebHookManager implements GitWebHookManager {

    private static final Logger log = Logger.getLogger(GitlabWebHookManager.class);

    @Inject
    WebsiteRepository websiteRepository;

    @Override
    public String getEventHeader(HttpServerRequest request) {
        return WebHookResource.getHeader(request, "X-Gitlab-Event");
    }

    @Override
    public boolean canHandleRequest(HttpServerRequest request) {
        String eventName = getEventHeader(request);
        log.tracef("X-Gitlab-Event=%s", eventName);
        return StringUtils.isNotEmpty(eventName);
    }

    @Override
    public boolean isMergeRequest(HttpServerRequest request) {
        return StringUtils.equals("Merge Request Hook", getEventHeader(request));
    }

    @Override
    public MergeStatus getMergeStatus(JsonObject data) {
        String state = data.getJsonObject("object_attributes").getString("state");
        String action = data.getJsonObject("object_attributes").getString("action");

        if (StringUtils.equals(state, "opened")) {
            return StringUtils.equals(action, "update") ? MergeStatus.UPDATE : MergeStatus.OPEN;
        }
        if (StringUtils.equals(state, "closed")){
            return MergeStatus.CLOSE;
        }
        return null;
    }

    @Override
    public void validateRequest(HttpServerRequest request, JsonObject data) throws NotAuthorizedException {
        String secretToken = WebHookResource.getHeader(request, "X-Gitlab-Token");

        if (StringUtils.isEmpty(secretToken)) {
            log.warn("X-Gitlab-Token is missing!");
            throw new NotAuthorizedException("X-Gitlab-Token missing", "token");
        }

        if (StringUtils.isEmpty(getGitUrl(request, data))) {
            throw new BadRequestException("Unsupported Event");
        }
        if (isMergeRequest(request)) {
            if (StringUtils.isEmpty(getPreviewGitUrl(data)) || StringUtils.isEmpty(getPreviewRef(data))) {
                throw new BadRequestException("Merge Event Data missing");
            }
        } else {
            if (StringUtils.isEmpty(getRef(data))) {
                throw new BadRequestException("Ref or branch not defined");
            }
        }
    }

    @Override
    public List<Website> getAuthorizedWebsites(HttpServerRequest request, JsonObject event) {
        String gitUrl = getGitUrl(request, event);
        String signature = WebHookResource.getHeader(request, "X-Gitlab-Token");

        return websiteRepository.getByGitUrl(gitUrl, signature, false);
    }

    @Override
    public String getGitUrl(HttpServerRequest request, JsonObject postData) {
        if (isMergeRequest(request)) {
            if (postData.containsKey("project")) {
                return postData.getJsonObject("project").getString("git_http_url");
            }
        }

        if (postData != null && postData.containsKey("repository")) {
            return postData.getJsonObject("repository").getString("git_http_url");
        }
        return null;
    }

    @Override
    public String getPreviewGitUrl(JsonObject postData) {
        if (postData.containsKey("object_attributes")) {
            return postData.getJsonObject("object_attributes").getJsonObject("source").getString("git_http_url");
        }
        return null;
    }

    @Override
    public String getRef(JsonObject postData) {
        if (postData != null && postData.containsKey("ref")) {
            return postData.getString("ref");
        }
        return null;
    }

    @Override
    public String getPreviewRef(JsonObject postData) {
        if (postData != null && postData.containsKey("object_attributes")) {
            return postData.getJsonObject("object_attributes").getString("source_branch");
        }
        return null;
    }

    @Override
    public String getPreviewId(JsonObject postData) {
        if (postData != null && postData.containsKey("object_attributes")) {
            return postData.getJsonObject("object_attributes").getInteger("iid").toString();
        }
        return null;
    }
}
