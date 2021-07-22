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

    @Override
    public String getEventHeader(HttpServerRequest request) {
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
        return StringUtils.isNotEmpty(data.getString("zen"));
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


    /**
     * TODO remove this method from this class
     * clarification : this method violets the single responsibility principal, it would be great if we can pass the Url
     * and signature to the invoker and invoker will retrieve the websiteRepository based on those values.
     * Look at the Return type of this method List of Websites is not a responsibility of WebHookManager, a WebHokManager
     * already deals with the complexity with request parsing and other stuffs
     */
    @Override
    public List<Website> getAuthorizedWebsites(HttpServerRequest request, JsonObject reqBody) {
        String gitUrl = getGitUrl(request, reqBody);
        String gitHubHmacHash = WebHookResource.getHeader(request, "X-Hub-Signature-256");
        return websiteRepository.getByGitUrl(gitUrl, reqBody.encode(), gitHubHmacHash);
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

    // GitHub issue 65
    @Override
    public JsonObject extractRepositoryInformation(JsonObject data) {
        var repositoryMeta = new JsonObject();
        repositoryMeta.put("projectUrl", data.getJsonObject("pull_request").getString("url"));
        repositoryMeta.put("repoType", RepoType.GITHUB);
        return repositoryMeta;
    }
    // GitHub issue 65
}
