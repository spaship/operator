package io.websitecd.operator.webhook.github;

import io.quarkus.security.UnauthorizedException;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.controller.OperatorService;
import io.websitecd.operator.rest.WebHookResource;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class GithubWebHookManager {

    private static final Logger log = Logger.getLogger(GithubWebHookManager.class);

    @Inject
    OperatorService operatorService;

    public boolean isGithubEvent(HttpServerRequest request) {
        List<String> eventName = request.headers().getAll("X-GitHub-Event");
        log.tracef("X-GitHub-Event=%s", eventName);
        if (eventName != null && eventName.size() > 0 && StringUtils.isNotEmpty(eventName.get(0))) {
            return true;
        }
        return false;
    }

    public Future<JsonObject> handleRequest(HttpServerRequest request, Buffer postData) {
        String eventName = WebHookResource.getHeader(request, "X-GitHub-Event");
        if (eventName == null || eventName.trim().isEmpty()) {
            log.warn("X-GitHub-Event header is missing!");
            return Future.failedFuture(new GithubException("X-GitHub-Event header is missing!"));
        }

        String signature = WebHookResource.getHeader(request, "X-Hub-Signature-256");

        if (StringUtils.isEmpty(signature)) {
            log.warn("X-Hub-Signature-256 is missing!");
            return Future.failedFuture(new UnauthorizedException("X-Hub-Signature-256 missing"));
        }

        log.infof("handleEvent: X-Gitlab-Event=%s remote_address=%s", eventName, request.remoteAddress());

        if (!eventName.equals("push")) {
            return Future.failedFuture(new BadRequestException("X-GitHub-Event not supported"));
        }

        try {
            JsonObject event = unmarshal(postData);
            return processEvent(event, signature);
        } catch (IOException e) {
            return Future.failedFuture(e);
        }
    }

    protected JsonObject unmarshal(Buffer data) throws IOException {
        return data.toJsonObject();
    }

    protected Future<JsonObject> processEvent(JsonObject event, String signature) {
        String gitUrl = event.getJsonObject("repository").getString("clone_url");


        return operatorService.rollout(gitUrl, signature, true);
    }

}
