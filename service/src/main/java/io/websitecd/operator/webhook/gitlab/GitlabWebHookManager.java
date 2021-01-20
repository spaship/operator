package io.websitecd.operator.webhook.gitlab;

import io.quarkus.runtime.StartupEvent;
import io.websitecd.operator.webhook.WebhookService;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.utils.JacksonJson;
import org.gitlab4j.api.webhook.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.InputStreamReader;

@ApplicationScoped
public class GitlabWebHookManager extends WebHookManager {

    private static final Logger log = Logger.getLogger(GitlabWebHookManager.class);

    private final JacksonJson jacksonJson = new JacksonJson();

    @Inject
    GitlabEventListener listener;

    void onStart(@Observes StartupEvent ev) {
        log.info("Registering Gitlab webhook listener");
        addListener(listener);
    }

    public static String getHeader(HttpRequest request, String name) {
        return WebhookService.getHeader(request, name);
    }

    /**
     * Parses and verifies an Event instance from the HTTP request and
     * fires it off to the registered listeners.
     *
     * @param request the HttpServletRequest to read the Event instance from
     * @return the Event instance that was read from the request body, null if the request
     * not contain a webhook event
     * @throws GitLabApiException if the parsed event is not supported
     */
    public Event handleRequest(HttpRequest request, String postData) throws GitLabApiException {

        String eventName = getHeader(request, "X-Gitlab-Event");
        if (eventName == null || eventName.trim().isEmpty()) {
            log.warn("X-Gitlab-Event header is missing!");
            return (null);
        }

        String secretToken = getHeader(request, "X-Gitlab-Token");

        if (secretToken == null) {
            log.warn("X-Gitlab-Token is missing!");
            return null;
        }

        if (!isValidSecretToken(secretToken)) {
            String message = "X-Gitlab-Token mismatch!";
            log.warn(message);
            throw new GitLabApiException(message);
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
                throw new GitLabApiException(message);
        }

        Event event;
        try {

            if (log.isEnabled(Logger.Level.TRACE)) {
                log.trace("Raw POST data:\n" + postData);
                event = jacksonJson.unmarshal(Event.class, postData);
                log.trace(event.getObjectKind() + " event:\n" + jacksonJson.marshal(event) + "\n");
            } else {
                InputStreamReader reader = new InputStreamReader(request.getInputStream());
                event = jacksonJson.unmarshal(Event.class, reader);
            }

        } catch (Exception e) {
            log.warn(String.format("Error processing JSON data, exception=%s, error=%s",
                    e.getClass().getSimpleName(), e.getMessage()));
            throw new GitLabApiException(e);
        }

        try {

            event.setRequestUrl(request.getUri().getRequestUri().toString());
//            event.setRequestQueryString(request.getQueryString());

            event.setRequestSecretToken(secretToken);

            fireEvent(event);
            return (event);

        } catch (Exception e) {
            log.warn(String.format("Error processing event, exception=%s, error=%s",
                    e.getClass().getSimpleName(), e.getMessage()));
            throw e;
        }
    }


}
