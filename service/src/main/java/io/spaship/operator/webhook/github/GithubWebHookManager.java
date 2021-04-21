package io.spaship.operator.webhook.github;

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
public class GithubWebHookManager implements GitWebHookManager {

    private static final Logger log = Logger.getLogger(GithubWebHookManager.class);

    @Inject
    WebsiteRepository websiteRepository;

    protected String getEventHeader(HttpServerRequest request) {
        return WebHookResource.getHeader(request, "X-GitHub-Event");
    }

    @Override
    public boolean canHandleRequest(HttpServerRequest request) {
        String eventName = getEventHeader(request);
        log.tracef("X-GitHub-Event=%s", eventName);
        return StringUtils.isNotEmpty(eventName);
    }

    @Override
    public boolean isMergeRequest(HttpServerRequest request) {
        return StringUtils.equals("pull_request", getEventHeader(request));
    }

    @Override
    public MergeStatus getMergeStatus(JsonObject data) {
        String action = data.getString("action");

        if (StringUtils.equals(action, "opened")) {
            return MergeStatus.OPEN;
        }
        if (StringUtils.equals(action, "closed")) {
            return MergeStatus.CLOSE;
        }
        return MergeStatus.UPDATE;
    }

    public boolean isPingRequest(JsonObject data) {
        if (StringUtils.isNotEmpty(data.getString("zen"))) {
            return true;
        }
        return false;
    }

    @Override
    public void validateRequest(HttpServerRequest request, JsonObject data) throws NotAuthorizedException, BadRequestException {
        String eventName = WebHookResource.getHeader(request, "X-GitHub-Event");
        if (eventName == null || eventName.trim().isEmpty()) {
            log.warn("X-GitHub-Event header is missing!");
            throw new BadRequestException("X-GitHub-Event header is missing!");
        }

        String signature = WebHookResource.getHeader(request, "X-Hub-Signature-256");

        if (StringUtils.isEmpty(signature)) {
            log.warn("X-Hub-Signature-256 is missing!");
            throw new NotAuthorizedException("X-Hub-Signature-256 missing", "token");
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
        String signature = WebHookResource.getHeader(request, "X-Hub-Signature-256");

        return websiteRepository.getByGitUrl(gitUrl, signature, true);
    }

    @Override
    public String getGitUrl(HttpServerRequest request, JsonObject postData) {
        if (postData != null && postData.containsKey("repository")) {
            return postData.getJsonObject("repository").getString("clone_url");
        }
        return null;
    }

    @Override
    public String getPreviewGitUrl(JsonObject postData) {
        if (postData != null && postData.containsKey("pull_request")) {
            return postData.getJsonObject("pull_request").getJsonObject("head").getJsonObject("repo").getString("clone_url");
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
        return postData.getJsonObject("pull_request").getJsonObject("head").getString("ref");
    }

    @Override
    public String getPreviewId(JsonObject postData) {
        return postData.getInteger("number").toString();
    }
}
