package io.websitecd.operator.webhook.gitlab;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.websitecd.content.git.config.GitContentUtils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.controller.WebsiteController;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.openshift.GitWebsiteConfigService;
import io.websitecd.operator.openshift.OperatorService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.gitlab4j.api.GitLabApiException;
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
    GitWebsiteConfigService gitWebsiteConfigService;

    @Inject
    WebsiteRepository websiteRepository;

    @Inject
    OperatorService operatorService;

    @Inject
    ContentController contentController;

    @ConfigProperty(name = "app.operator.website.config.filenames")
    String[] websiteYamlName;

    @ConfigProperty(name = "app.content.git.rootcontext")
    protected String rootContext;

    public Uni<JsonObject> onPushEvent(PushEvent pushEvent) throws GitLabApiException {
        String gitUrl = pushEvent.getRepository().getGit_http_url();
        return handleEvent(gitUrl, pushEvent);
    }

    public Uni<JsonObject> onTagPushEvent(TagPushEvent tagPushEvent) throws GitLabApiException {
        String gitUrl = tagPushEvent.getRepository().getGit_http_url();
        return handleEvent(gitUrl, tagPushEvent);
    }

    public Uni<JsonObject> handleEvent(String gitUrl, Event event) {
        List<Website> websites = websiteRepository.getByGitUrl(gitUrl, event.getRequestSecretToken());
//        boolean isComponent = websiteConfigService.isKnownComponent(gitUrl);
        JsonObject resultObject = new JsonObject();

        boolean isWebsite = websites.size() > 0;
        if (!isWebsite) {
            log.infof("website with given gitUrl and token not found. ignoring. gitUrl=%s", gitUrl);
            return Uni.createFrom().item(resultObject.put("status", "IGNORED").put("reason", "pair git url and token unknown"));
        }

        boolean rollout = false;
        resultObject.put("status", "SUCCESS").put("components", new JsonArray());

        List<Publisher<JsonObject>> updates = new ArrayList<>();
        for (Website website : websites) {
            log.infof("Update website=%s", website);
            WebsiteConfig websiteConfig = website.getConfig();
            if (isWebsite && isRolloutNeeded(event, websiteYamlName)) {
                try {
                    WebsiteConfig newConfig = gitWebsiteConfigService.updateRepo(website);
                    if (WebsiteController.deploymentChanged(websiteConfig, newConfig)) {
                        operatorService.initInfrastructure(website, true, false);
                        rollout = true;
                        resultObject.put("website", new JsonObject().put("name", newConfig.getWebsiteName()).put("gitUrl", gitUrl));
                    }
                    websiteConfig = newConfig;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (!rollout) {
                Map<String, Environment> envs = websiteConfig.getEnvs();
                for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
                    String env = envEntry.getKey();
                    if (!operatorService.isEnvEnabled(envEntry.getValue(), website.getMetadata().getNamespace())) {
                        log.debug("Env is not enabled");
                        continue;
                    }
                    List<String> names = new ArrayList<>();
                    for (ComponentConfig component : websiteConfig.getComponents()) {
                        if (!component.isKindGit()) {
                            continue;
                        }
                        if (!OperatorConfigUtils.isComponentEnabled(websiteConfig, env, component.getContext())) {
                            continue;
                        }
                        String componentDir = GitContentUtils.getDirName(component.getContext(), rootContext);
                        names.add(componentDir);
                    }
                    if (names.size() > 0) {
                        Multi<JsonObject> update = updateAllComponents(website, env);
                        updates.add(update);
                    }
                }
            }
        }

        Multi<JsonObject> merged = Multi.createBy().merging().streams(updates);

        Uni<JsonObject> result = merged.collectItems()
                .in(() -> resultObject, (arr, obj) -> arr.getJsonArray("components").add(obj));

        return result;
    }

//    protected Multi<JsonObject> updateComponents(Website website, String env, List<String> names) {
//        String gitUrl = website.getSpec().getGitUrl();
//        WebClient contentApiClient = contentController.getContentApiClient(website, env);
//        if (contentApiClient == null) {
//            throw new RuntimeException("contentApiClient not defined for gitUrl=" + gitUrl);
//        }
//        log.infof("Update components on websiteId=%s env=%s, names=%s", website.getId(), env, names);
//        return Multi.createFrom().items(names.toArray(new String[0]))
//                .onItem().invoke(name -> {
//                    log.debugf("Going to update component name=%s", name);
//                    contentController.refreshComponent(contentApiClient, name)
//                            .subscribe()
//                            .with(s -> log.infof("updated name=%s result=%s", name, s), err -> {
//                                log.error("Error update", err);
//                                Uni.createFrom().failure(err);
//                            });
//                })
//                .onFailure(err -> {
//                    log.error("eee", err);
//                    return false;
//                }).recoverWithItem("error")
//                .onItem().transform(name -> new JsonObject().put("gitUrl", gitUrl).put("env", env).put("component", name));
//    }

    protected Multi<JsonObject> updateAllComponents(Website website, String env) {
        String gitUrl = website.getSpec().getGitUrl();
        WebClient contentApiClient = contentController.getContentApiClient(website, env);
        if (contentApiClient == null) {
            throw new RuntimeException("contentApiClient not defined for gitUrl=" + gitUrl);
        }
        log.infof("Update all components on websiteId=%s env=%s", website.getId(), env);
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
