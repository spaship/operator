package io.websitecd.operator.webhook.gitlab;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.websitecd.content.git.config.GitContentUtils;
import io.websitecd.operator.config.model.ComponentConfig;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.controller.OperatorService;
import io.websitecd.operator.controller.WebsiteRepository;
import io.websitecd.operator.crd.Website;
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
import java.util.stream.Collectors;

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

    @Inject
    Vertx vertx;

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
            return rollout(gitUrl, event.getRequestSecretToken());
        }

        log.infof("Update components with same gitUrl and branch. gitUrl=%s ref=%s", gitUrl, ref);
        List<Future> updates = updateRelatedComponents(gitUrl, ref);
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

    public List<Future> updateRelatedComponents(String gitUrl, String ref) {
        List<Future> updates = new ArrayList<>();
        for (Website website : websiteRepository.getWebsites().values()) {
            // secret token is not checked
            for (ComponentConfig component : website.getConfig().getComponents()) {
                if (!component.isKindGit() || !StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                    continue;
                }
                log.tracef("Component with same gitUrl found. context=%s", component.getContext());
                List<Future<JsonObject>> componentsUpdates = website.getEnabledEnvs().stream()
                        .filter(env -> {
                            String componentRef = GitContentUtils.getRef(website.getConfig(), env, component.getContext());
                            return ref.contains(componentRef);
                        })
                        .map(env -> contentController.refreshComponent(website, env, GitContentUtils.getDirName(component.getContext(), rootContext)))
                        .collect(Collectors.toList());
                updates.addAll(componentsUpdates);
            }
        }
        return updates;
    }

    public Future<JsonObject> rollout(String gitUrl, String requestSecretToken) {
        List<Website> websites = websiteRepository.getByGitUrl(gitUrl, requestSecretToken);
        JsonObject resultObject = new JsonObject();

        if (websites.size() == 0) {
            throw new BadRequestException("website with given gitUrl and token not found.");
        }

        JsonArray updatedSites = new JsonArray();
        for (Website website : websites) {
            rolloutWebsiteNonBlocking(website);
            updatedSites.add(new JsonObject().put("name", website.getMetadata().getName()).put("namespace", website.getMetadata().getNamespace()));
        }
        resultObject.put("status", STATUS_SUCCESS)
                .put("websites", updatedSites);
        return Future.succeededFuture(resultObject);
    }

    public void rolloutWebsiteNonBlocking(Website website) {
        String websiteId = website.getId();
        vertx.executeBlocking(future -> {
            log.infof("Rollout websiteId=%s", websiteId);
            try {
                WebsiteConfig newConfig = gitWebsiteConfigService.updateRepo(website);
                website.setConfig(newConfig);
                websiteRepository.addWebsite(website);

                operatorService.initInfrastructure(website, true);
                future.complete();
            } catch (Exception e) {
                future.fail(e.getMessage());
            }
        }, res -> {
            if (res.succeeded()) {
                log.infof("Website updated websiteId=%s", websiteId);
            } else {
                log.error("Cannot update website, websiteId=" + websiteId, res.cause());
            }
        });
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
