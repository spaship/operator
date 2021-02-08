package io.websitecd.operator.webhook.gitlab;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.webhook.WebhookService;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.utils.JacksonJson;
import org.gitlab4j.api.webhook.*;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class GitlabWebHookManager extends WebHookManager {

    private static final Logger log = Logger.getLogger(GitlabWebHookManager.class);

    private final JacksonJson jacksonJson = new JacksonJson();

    @Inject
    GitlabWebHookListener listener;

    public static String getHeader(HttpServerRequest request, String name) {
        return WebhookService.getHeader(request, name);
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

        String eventName = getHeader(request, "X-Gitlab-Event");
        if (eventName == null || eventName.trim().isEmpty()) {
            log.warn("X-Gitlab-Event header is missing!");
            return Future.failedFuture(new GitLabApiException("X-Gitlab-Event header is missing!"));
        }

        String secretToken = getHeader(request, "X-Gitlab-Token");

        if (!isValidSecretToken(secretToken)) {
            String message = "X-Gitlab-Token mismatch!";
            log.warn(message);
            return Future.failedFuture(new GitLabApiException(message));
        }

        log.infof("handleEvent: X-Gitlab-Event=%s", eventName);
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
                log.info(message);
                return Future.failedFuture(new GitLabApiException(message));
        }
    }

    protected Event unmarshal(Buffer data) throws IOException {
        return jacksonJson.unmarshal(Event.class, data.toString());
    }

}
