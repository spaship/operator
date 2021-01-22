package io.websitecd.operator.webhook.gitlab;

import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.WebsiteConfigService;
import io.websitecd.operator.webhook.WebhookService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.utils.JacksonJson;
import org.gitlab4j.api.webhook.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class GitlabWebHookManager extends WebHookManager {

    private static final Logger log = Logger.getLogger(GitlabWebHookManager.class);

    private final JacksonJson jacksonJson = new JacksonJson();

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    OperatorService operatorService;

    @Inject
    ContentController contentController;

    @ConfigProperty(name = "app.operator.website.config.filename")
    String websiteYamlName;


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
            event = unmarshal(postData);
            if (log.isEnabled(Logger.Level.TRACE)) {
                log.trace(event.getObjectKind() + " event:\n" + jacksonJson.marshal(event) + "\n");
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

    protected Event unmarshal(String data) throws IOException {
        return jacksonJson.unmarshal(Event.class, data);
    }

    @Override
    public void fireEvent(Event event) throws GitLabApiException {
        if (isRolloutNeeded(event)) {
            WebsiteConfig oldConfig = websiteConfigService.getConfig();
            try {
                WebsiteConfig newConfig = websiteConfigService.updateRepo();
                if (deploymentChanged(oldConfig, newConfig)) {
                    operatorService.processConfig(newConfig);
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            contentController.listComponents()
                    .onItem().transform(jsonArray -> jsonArray.getList())
                    .subscribe()
                    .with(items -> {
                        for (Object name : items) {
                            contentController.refreshComponent(name.toString())
                                    .subscribe()
                                    .with(s -> log.infof("updated name=%s result=%s", name, s));
                        }
                    });
        }
        super.fireEvent(event);
    }

    public boolean isRolloutNeeded(Event event) {
        if (event instanceof PushEvent) {
            PushEvent pushEvent = (PushEvent) event;
            for (EventCommit commit : pushEvent.getCommits()) {
                if (commit.getModified().contains(websiteYamlName)) {
                    return true;
                }
                if (commit.getAdded().contains(websiteYamlName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deploymentChanged(WebsiteConfig oldConfig, WebsiteConfig newConfig) {
        // TODO: Compare old and new config and consider if deployment has changed
        return true;
    }
}
