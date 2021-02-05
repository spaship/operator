package io.websitecd.operator.webhook.gitlab;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.controller.WebsiteController;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.WebsiteConfigService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.gitlab4j.api.webhook.Event;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GitlabWebHookListener {

    private static final Logger log = Logger.getLogger(GitlabWebHookListener.class);


    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    OperatorService operatorService;

    @Inject
    ContentController contentController;

    @ConfigProperty(name = "app.operator.website.config.filenames")
    String[] websiteYamlName;

    public Uni<JsonObject> onPushEvent(PushEvent pushEvent) {
        String gitUrl = pushEvent.getRepository().getGit_http_url();
        return handleEvent(gitUrl, pushEvent);
    }

    public Uni<JsonObject> onTagPushEvent(TagPushEvent tagPushEvent) {
        String gitUrl = tagPushEvent.getRepository().getGit_http_url();
        return handleEvent(gitUrl, tagPushEvent);
    }

    public Uni<JsonObject> handleEvent(String gitUrl, Event event) {
        boolean isWebsite = websiteConfigService.isKnownWebsite(gitUrl);
        boolean isComponent = websiteConfigService.isKnownComponent(gitUrl);
        JsonObject resultObject = new JsonObject();
        if (!isWebsite || !isComponent) {
            log.infof("git url unknown. ignoring. gitUrl=%s", gitUrl);
            return Uni.createFrom().item(resultObject.put("status", "IGNORED").put("reason", "git url unknown"));
        }

        boolean rollout = false;
        resultObject.put("status", "SUCCESS").put("components", new JsonArray());

        if (isWebsite && isRolloutNeeded(event, websiteYamlName)) {
            WebsiteConfig oldConfig = websiteConfigService.getConfig(gitUrl);
            try {
                WebsiteConfig newConfig = websiteConfigService.updateRepo(gitUrl);
                if (WebsiteController.deploymentChanged(oldConfig, newConfig)) {
                    operatorService.processConfig(gitUrl, true, false, null);
                    rollout = true;
                    resultObject.put("website", new JsonObject().put("name", newConfig.getWebsiteName()).put("gitUrl", gitUrl));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        List<Publisher<JsonObject>> updates = new ArrayList<>();
        for (Map.Entry<String, WebsiteConfig> configEntry : websiteConfigService.getWebsites().entrySet()) {
            if (rollout && StringUtils.equals(configEntry.getKey(), gitUrl)) {
                log.debugf("component is already covered by rolling update. going to next component. ");
                continue;
            }
            WebsiteConfig config = configEntry.getValue();
            Map<String, Environment> envs = config.getEnvs();
            for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
                if (!operatorService.isEnvEnabled(envEntry.getValue(), null)) {
                    log.debugf("Env is not enabled");
                    continue;
                }
                Multi<JsonObject> update = updateAllComponents(gitUrl, envEntry.getKey());
                updates.add(update);
            }
        }
        Multi<JsonObject> merged = Multi.createBy().merging().streams(updates);

        Uni<JsonObject> result = merged.collectItems()
                .in(() -> resultObject, (arr, obj) -> arr.getJsonArray("components").add(obj));

        return result;
    }


    protected Multi<JsonObject> updateAllComponents(String gitUrl, String env) {
        String clientId = gitUrl + "-" + env;
        WebClient contentApiClient = contentController.getContentApiClient(clientId);
        if (contentApiClient == null) {
            throw new RuntimeException("contentApiClient not defined for gitUrl=" + clientId);
        }
        log.infof("Update all components on clientId=%s ", clientId);
        return contentController.listComponents(contentApiClient)
                .invoke(name -> {
                    log.debugf("Going to update component name=%s", name);
                    contentController.refreshComponent(contentApiClient, name)
                            .subscribe()
                            .with(s -> log.infof("updated name=%s result=%s", name, s), err -> Uni.createFrom().failure(err));
                })
                .onItem().transform(name -> new JsonObject().put("gitUrl", gitUrl).put("env", env).put("component", name));
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
