package io.websitecd.operator.webhook.gitlab;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.controller.OperatorService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.gitlab4j.api.webhook.Event;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.List;

import static io.websitecd.operator.webhook.WebhookService.STATUS_SUCCESS;

@ApplicationScoped
public class GitlabWebHookListener {

    private static final Logger log = Logger.getLogger(GitlabWebHookListener.class);

    @Inject
    OperatorService operatorService;

    @ConfigProperty(name = "app.operator.website.config.filenames")
    String[] websiteYamlName;

    public Future<JsonObject> onPushEvent(PushEvent pushEvent) {
        String gitUrl = pushEvent.getRepository().getGit_http_url();
        String ref = pushEvent.getRef();
        return handleEvent(gitUrl, ref, pushEvent);
    }

    public Future<JsonObject> onTagPushEvent(TagPushEvent tagPushEvent) {
        String gitUrl = tagPushEvent.getRepository().getGit_http_url();
        String ref = tagPushEvent.getRef();
        return handleEvent(gitUrl, ref, tagPushEvent);
    }

    public Future<JsonObject> handleEvent(String gitUrl, String ref, Event event) {
        boolean isRollout = isRolloutNeeded(event, websiteYamlName);
        if (isRollout) {
            return operatorService.rollout(gitUrl, event.getRequestSecretToken());
        }

        log.infof("Update components with same gitUrl and branch. gitUrl=%s ref=%s", gitUrl, ref);
        List<Future> updates = operatorService.updateRelatedComponents(gitUrl, ref);
        if (updates.size() == 0) {
            return Future.failedFuture(new BadRequestException("no matched website or components"));
        }

        JsonObject resultObject = new JsonObject().put("status", STATUS_SUCCESS).put("components", new JsonArray());

        Promise<JsonObject> promise = Promise.promise();
        CompositeFuture.join(updates)
                .onFailure(promise::fail)
                .onSuccess(e -> {
                    if (e.result().list() != null) {
                        resultObject.put("components", e.result().list());
                    }
                    promise.complete(resultObject);
                });
        return promise.future();
    }

    public static boolean isRolloutNeeded(Event event, String... yamlNames) {
        if (event instanceof PushEvent) {
            PushEvent pushEvent = (PushEvent) event;
            for (EventCommit commit : pushEvent.getCommits()) {
                if (containsString(commit.getModified(), yamlNames)) {
                    return true;
                }
                if (containsString(commit.getAdded(), yamlNames)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsString(List<String> list, String... searchStrings) {
        if (list == null || list.size() == 0) {
            return false;
        }
        for (String s : list) {
            if (StringUtils.containsAny(s, searchStrings)) {
                return true;
            }
        }
        return false;
    }

}
