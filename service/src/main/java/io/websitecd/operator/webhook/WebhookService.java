package io.websitecd.operator.webhook;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.webhook.gitlab.GitlabWebHookManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

@ApplicationScoped
public class WebhookService {

    public static final String STATUS_SUCCESS = "SUCCESS";

    @Inject
    GitlabWebHookManager gitlabWebHookManager;

    public Future<JsonObject> handleRequest(HttpServerRequest request, Buffer data) {
        if (gitlabWebHookManager.isGitlabEvent(request)) {
            return gitlabWebHookManager.handleRequest(request, data);
        }

        return Future.failedFuture(new BadRequestException("unknown provider"));
    }

}
