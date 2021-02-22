package io.websitecd.operator.webhook.gitlab;

import io.quarkus.security.UnauthorizedException;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.rest.WebHookResource;
import io.websitecd.operator.webhook.GitWebHookManager;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.List;

@ApplicationScoped
public class GitlabWebHookManager implements GitWebHookManager {

    private static final Logger log = Logger.getLogger(GitlabWebHookManager.class);

    @Inject
    WebsiteRepository websiteRepository;

    @Override
    public boolean canHandleRequest(HttpServerRequest request) {
        List<String> eventName = request.headers().getAll("X-Gitlab-Event");
        log.tracef("X-Gitlab-Event=%s", eventName);
        if (eventName != null && eventName.size() > 0 && StringUtils.isNotEmpty(eventName.get(0))) {
            return true;
        }
        return false;
    }

    @Override
    public void validateRequest(HttpServerRequest request, JsonObject data) throws UnauthorizedException {
        String secretToken = WebHookResource.getHeader(request, "X-Gitlab-Token");

        if (StringUtils.isEmpty(secretToken)) {
            log.warn("X-Gitlab-Token is missing!");
            throw new UnauthorizedException("X-Gitlab-Token missing");
        }

        if (StringUtils.isEmpty(getGitUrl(data)) || StringUtils.isEmpty(getRef(data))) {
            throw new BadRequestException("Unsupported Event");
        }
    }

    @Override
    public List<Website> getAuthorizedWebsites(HttpServerRequest request, JsonObject event) {
        String gitUrl = getGitUrl(event);
        String signature = WebHookResource.getHeader(request, "X-Gitlab-Token");

        return websiteRepository.getByGitUrl(gitUrl, signature, false);
    }

    @Override
    public String getGitUrl(JsonObject postData) {
        if (postData != null && postData.containsKey("repository")) {
            return postData.getJsonObject("repository").getString("git_http_url");
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

}
