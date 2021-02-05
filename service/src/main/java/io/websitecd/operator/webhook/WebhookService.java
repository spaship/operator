package io.websitecd.operator.webhook;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.webhook.gitlab.GitlabWebHookManager;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gitlab4j.api.GitLabApiException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class WebhookService {

    private static final Logger log = Logger.getLogger(WebhookService.class);

    public enum GIT_PROVIDER {
        GITLAB
    }

    @Inject
    GitlabWebHookManager gitlabWebHookManager;

    public GIT_PROVIDER gitProvider(HttpRequest request) {
        List<String> eventName = request.getHttpHeaders().getRequestHeader("X-Gitlab-Event");
        log.tracef("X-Gitlab-Event=%s", eventName);
        if (eventName != null && eventName.size() > 0 && StringUtils.isNotEmpty(eventName.get(0))) {
            return GIT_PROVIDER.GITLAB;
        }
        return null;
    }

    public Uni<JsonObject> handleGitlab(HttpRequest request, String data) throws GitAPIException, IOException, GitLabApiException {
        String secretToken = getHeader(request, "X-Gitlab-Token");

        if (StringUtils.isEmpty(secretToken)) {
            log.warn("X-Gitlab-Token is missing!");
            throw new UnauthorizedException("X-Gitlab-Token missing");
        }

        return gitlabWebHookManager.handleRequest(request, data);
    }

    public static String getHeader(HttpRequest request, String name) {
        List<String> headers = request.getHttpHeaders().getRequestHeader(name);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        return StringUtils.trimToNull(headers.get(0));
    }
}
