package io.websitecd.operator.webhook.gitlab;

import io.quarkus.security.UnauthorizedException;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.rest.WebHookResource;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.utils.JacksonJson;
import org.gitlab4j.api.webhook.*;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class GitlabWebHookManager extends WebHookManager {

    private static final Logger log = Logger.getLogger(GitlabWebHookManager.class);

    private final JacksonJson jacksonJson = new JacksonJson();

    @Inject
    GitlabWebHookListener listener;

    public boolean isGitlabEvent(HttpServerRequest request) {
        List<String> eventName = request.headers().getAll("X-Gitlab-Event");
        log.tracef("X-Gitlab-Event=%s", eventName);
        if (eventName != null && eventName.size() > 0 && StringUtils.isNotEmpty(eventName.get(0))) {
            return true;
        }
        return false;
    }

    /**
     * Parses and verifies an Event instance from the HTTP request and
     * fires it off to the registered listeners.
     *
     * @param request  the HttpServletRequest to read the Event instance from
     * @param postData
     * @return the Event instance that was read from the request body, null if the request
     * not contain a webhook event
     */
    public Future<JsonObject> handleRequest(HttpServerRequest request, Buffer postData) {
        String eventName = WebHookResource.getHeader(request, "X-Gitlab-Event");
        if (eventName == null || eventName.trim().isEmpty()) {
            log.warn("X-Gitlab-Event header is missing!");
            return Future.failedFuture(new GitLabApiException("X-Gitlab-Event header is missing!"));
        }

        String secretToken = WebHookResource.getHeader(request, "X-Gitlab-Token");

        if (StringUtils.isEmpty(secretToken)) {
            log.warn("X-Gitlab-Token is missing!");
            return Future.failedFuture(new UnauthorizedException("X-Gitlab-Token missing"));
        }

        if (!isValidSecretToken(secretToken)) {
            String message = "X-Gitlab-Token mismatch!";
            log.warn(message);
            return Future.failedFuture(new UnauthorizedException(message));
        }

        log.infof("handleEvent: X-Gitlab-Event=%s remote_address=%s", eventName, request.remoteAddress());
        switch (eventName) {

            case IssueEvent.X_GITLAB_EVENT:
            case JobEvent.JOB_HOOK_X_GITLAB_EVENT:
            case MergeRequestEvent.X_GITLAB_EVENT:
            case NoteEvent.X_GITLAB_EVENT:
            case PipelineEvent.X_GITLAB_EVENT:
            case PushEvent.X_GITLAB_EVENT:
            case TagPushEvent.X_GITLAB_EVENT:
            case WikiPageEvent.X_GITLAB_EVENT:
                break;

            default:
                String message = "Unsupported X-Gitlab-Event, event Name=" + eventName;
                log.warn(message);
                return Future.failedFuture(new GitLabApiException(message));
        }

        Event event;
        try {
            event = unmarshal(postData);
            if (log.isEnabled(Logger.Level.TRACE)) {
                log.trace(event.getObjectKind() + " event:\n" + jacksonJson.marshal(event) + "\n");
            }

        } catch (Exception e) {
            log.warn(String.format("Error processing JSON data, exception=%s, error=%s",
                    e.getClass().getSimpleName(), e.getMessage()));
            return Future.failedFuture(new GitLabApiException(e));
        }

        try {

            event.setRequestUrl(request.absoluteURI());
//            event.setRequestQueryString(request.getQueryString());

            event.setRequestSecretToken(secretToken);

            fireEvent(event);
            return processEvent(event);
        } catch (Exception e) {
            log.warn(String.format("Error processing event, exception=%s, error=%s",
                    e.getClass().getSimpleName(), e.getMessage()));
            return Future.failedFuture(e);
        }
    }

    public Future<JsonObject> processEvent(Event event) {
        switch (event.getObjectKind()) {
            case PushEvent.OBJECT_KIND:
                return listener.onPushEvent((PushEvent) event);
            case TagPushEvent.OBJECT_KIND:
                return listener.onTagPushEvent((TagPushEvent) event);
            default:
                String message = "Unsupported event object_kind, object_kind=" + event.getObjectKind();
                log.warn(message);
                return Future.failedFuture(new GitLabApiException(message));
        }
    }

    protected Event unmarshal(Buffer data) throws IOException {
        return jacksonJson.unmarshal(Event.class, data.toString());
    }

}
