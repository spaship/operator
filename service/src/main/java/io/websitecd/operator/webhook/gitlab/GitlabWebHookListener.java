package io.websitecd.operator.webhook.gitlab;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.websitecd.content.git.config.GitContentUtils;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.controller.OperatorService;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.websiteconfig.GitWebsiteConfigService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.websitecd.operator.webhook.WebhookService.STATUS_IGNORED;
import static io.websitecd.operator.webhook.WebhookService.STATUS_SUCCESS;

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

    public Future<JsonObject> onPushEvent(PushEvent pushEvent) {
        String gitUrl = pushEvent.getRepository().getGit_http_url();
        return handleEvent(gitUrl, pushEvent);
    }

    public Future<JsonObject> onTagPushEvent(TagPushEvent tagPushEvent) {
        String gitUrl = tagPushEvent.getRepository().getGit_http_url();
        return handleEvent(gitUrl, tagPushEvent);
    }

    public Future<JsonObject> handleEvent(String gitUrl, Event event) {
        String requestSecretToken = event.getRequestSecretToken();
        boolean isRollout = isRolloutNeeded(event, websiteYamlName);
        if (isRollout) {
            return rollout(gitUrl, event);
        }

        log.debugf("Trying to find websites with same token but gitUrl may be used as component");

        List<Future> updates = new ArrayList<>();
        for (Map.Entry<String, Website> entry : websiteRepository.getWebsites().entrySet()) {
            Website website = entry.getValue();
            WebsiteSpec spec = website.getSpec();
            if (!requestSecretToken.equals(spec.getSecretToken())) {
                log.debugf("skipping website id=%s", website.getId());
                continue;
            }
            boolean componentFound = false;
            for (ComponentConfig component : website.getConfig().getComponents()) {
                if (StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                    componentFound = true;
                    break;
                }
            }
            if (componentFound) {
                updates.addAll(getComponentUpdates(website, gitUrl));
            }
        }
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

    public Future<JsonObject> rollout(String gitUrl, Event event) {
        String requestSecretToken = event.getRequestSecretToken();
        List<Website> websites = websiteRepository.getByGitUrl(gitUrl, requestSecretToken);
        JsonObject resultObject = new JsonObject();

        if (websites.size() == 0) {
            log.infof("website with given gitUrl and token not found. ignoring. gitUrl=%s", gitUrl);
            resultObject.put("status", STATUS_IGNORED).put("reason", "pair git url and token unknown");
            return Future.succeededFuture(resultObject);
        }

        JsonArray updatedSites = new JsonArray();
        for (Website website : websites) {
            log.debugf("Check website=%s", website);
            WebsiteConfig websiteConfig = website.getConfig();
            try {
                WebsiteConfig newConfig = gitWebsiteConfigService.updateRepo(website);
                if (websiteConfig.equals(newConfig)) {
                    log.debugf("config has not changed. ignoring");
                    continue;
                }
                website.setConfig(newConfig);
                websiteRepository.addWebsite(website);

                operatorService.initInfrastructure(website, true);

                log.debugf("Website updated websiteId=%s", website.getId());
                updatedSites.add(new JsonObject().put("name", website.getMetadata().getName()).put("namespace", website.getMetadata().getNamespace()));
            } catch (Exception e) {
                return Future.failedFuture(e);
            }
        }
        resultObject.put("status", STATUS_SUCCESS)
                .put("websites", updatedSites);
        return Future.succeededFuture(resultObject);
    }

    public List<Future> getComponentUpdates(Website website, String gitUrl) {
        List<Future> updates = new ArrayList<>();
        WebsiteConfig websiteConfig = website.getConfig();
        Map<String, Environment> envs = websiteConfig.getEnvs();
        for (Map.Entry<String, Environment> envEntry : envs.entrySet()) {
            String env = envEntry.getKey();
            if (!website.isEnvEnabled(env)) {
                log.debug("Env is not enabled");
                continue;
            }
            List<String> names = new ArrayList<>();
            for (ComponentConfig component : websiteConfig.getComponents()) {
                if (!component.isKindGit()) {
                    continue;
                }
                if (StringUtils.isNotEmpty(component.getSpec().getUrl()) && !StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                    continue;
                }
                if (!OperatorConfigUtils.isComponentEnabled(websiteConfig, env, component.getContext())) {
                    continue;
                }
                String componentDir = GitContentUtils.getDirName(component.getContext(), rootContext);
                names.add(componentDir);
            }
            for (String name : names) {
                Future<JsonObject> update = contentController.refreshComponent(website, env, name);
                updates.add(update);
            }
        }
        return updates;
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
