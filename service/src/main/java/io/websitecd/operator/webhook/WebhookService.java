package io.websitecd.operator.webhook;

import io.quarkus.security.UnauthorizedException;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.webhook.gitlab.GitlabWebHookManager;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class WebhookService {

    private static final Logger log = Logger.getLogger(WebhookService.class);

    public enum GIT_PROVIDER {
        GITLAB
    }

    @Inject
    GitlabWebHookManager gitlabWebHookManager;

    public GIT_PROVIDER gitProvider(HttpServerRequest request) {
        List<String> eventName = request.headers().getAll("X-Gitlab-Event");
        log.tracef("X-Gitlab-Event=%s", eventName);
        if (eventName != null && eventName.size() > 0 && StringUtils.isNotEmpty(eventName.get(0))) {
            return GIT_PROVIDER.GITLAB;
        }
        return null;
    }

    public Future<JsonObject> handleGitlab(HttpServerRequest request, Buffer data) {
        String secretToken = getHeader(request, "X-Gitlab-Token");

        if (StringUtils.isEmpty(secretToken)) {
            log.warn("X-Gitlab-Token is missing!");
            return Future.failedFuture(new UnauthorizedException("X-Gitlab-Token missing"));
        }

        return gitlabWebHookManager.handleRequest(request, data);
    }

    public static String getHeader(HttpServerRequest request, String name) {
        List<String> headers = request.headers().getAll(name);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        return StringUtils.trimToNull(headers.get(0));
    }
}
