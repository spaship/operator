package io.websitecd.operator.webhook;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.controller.OperatorService;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.webhook.github.GithubWebHookManager;
import io.websitecd.operator.webhook.gitlab.GitlabWebHookManager;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class WebhookService {

    private static final Logger log = Logger.getLogger(WebhookService.class);

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_IGNORED = "IGNORED";
    public static final String STATUS_UPDATING = "UPDATING";
    public static final String STATUS_PING = "PING";

    @Inject
    GitlabWebHookManager gitlabWebHookManager;

    @Inject
    GithubWebHookManager githubWebHookManager;

    @Inject
    OperatorService operatorService;

    Set<GitWebHookManager> managers;

    void onStart(@Observes StartupEvent ev) {
        managers = Set.of(gitlabWebHookManager, githubWebHookManager);
    }

    public Future<JsonObject> handleRequest(HttpServerRequest request, JsonObject data) {
        JsonObject resultObject = new JsonObject();

        GitWebHookManager manager = getManager(request);
        if (manager == null) {
            if (githubWebHookManager.isPingRequest(data)) {
                log.infof("Ping event registered. gitUrl=%s", githubWebHookManager.getGitUrl(data));
                return Future.succeededFuture(resultObject.put("status", STATUS_PING));
            } else {
                return Future.failedFuture(new BadRequestException("unknown provider"));
            }
        }

        resultObject.put("status", STATUS_SUCCESS);

        List<JsonObject> updatedSites;
        boolean someWebsitesUpdated;
        try {
            manager.validateRequest(request, data);

            List<Website> websites = manager.getAuthorizedWebsites(request, data);

            updatedSites = handleWebsites(websites);
            resultObject.put("websites", updatedSites);
            someWebsitesUpdated = updatedSites.size() != 0;
        } catch (Exception e) {
            return Future.failedFuture(e);
        }

        Set<String> updatedWebsiteIds = updatedSites.stream().
                filter(site -> !STATUS_IGNORED.equals(site.getString("status")))
                .map(site -> site.getString("namespace") + "-" + site.getString("name"))
                .collect(Collectors.toSet());

        String gitUrl = manager.getGitUrl(data);
        String ref = manager.getRef(data);
        List<Future> updates = operatorService.updateRelatedComponents(gitUrl, ref, updatedWebsiteIds);
        log.infof("Update components with same gitUrl and branch. gitUrl=%s ref=%s matchedComponents=%s", gitUrl, ref, updates.size());
        if (updates.size() == 0 && !someWebsitesUpdated) {
            return Future.failedFuture(new BadRequestException("no matched website or components"));
        }

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

    protected List<JsonObject> handleWebsites(List<Website> websites) throws GitAPIException, IOException {
        List<JsonObject> updatedSites = new ArrayList<>();

        for (Website website : websites) {
            boolean updatePerformed = operatorService.rolloutWebsiteNonBlocking(website, false);
            JsonObject result = new JsonObject()
                    .put("name", website.getMetadata().getName())
                    .put("namespace", website.getMetadata().getNamespace())
                    .put("status", updatePerformed ? STATUS_UPDATING : STATUS_IGNORED);
            updatedSites.add(result);
        }
        return updatedSites;
    }

    private GitWebHookManager getManager(HttpServerRequest request) {
        for (GitWebHookManager manager : managers) {
            if (manager.canHandleRequest(request)) {
                return manager;
            }
        }
        return null;
    }

}
