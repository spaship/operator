package io.spaship.operator.webhook.github;

import io.spaship.operator.controller.WebsiteRepository;
import io.spaship.operator.crd.Website;
import io.spaship.operator.rest.WebHookResource;
import io.spaship.operator.webhook.GitWebHookManager;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;
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

    @Override
    public boolean canHandleRequest(HttpServerRequest request) {
        List<String> eventName = request.headers().getAll("X-GitHub-Event");
        log.tracef("X-GitHub-Event=%s", eventName);
        if (eventName != null && eventName.size() > 0 && StringUtils.isNotEmpty(eventName.get(0))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isMergeRequest(HttpServerRequest request) {
        return false;
    }

    @Override
    public MergeStatus getMergeStatus(JsonObject data) {
        throw new NotImplementedException();
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

        if (StringUtils.isEmpty(getGitUrl(request, data)) || StringUtils.isEmpty(getRef(data))) {
            throw new BadRequestException("Unsupported Event");
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
    public String getRef(JsonObject postData) {
        if (postData != null && postData.containsKey("ref")) {
            return postData.getString("ref");
        }
        return null;
    }

    @Override
    public String getPreviewRef(JsonObject postData) {
        throw new NotImplementedException();
    }

    @Override
    public String getPreviewId(JsonObject postData) {
        throw new NotImplementedException();
    }
}
