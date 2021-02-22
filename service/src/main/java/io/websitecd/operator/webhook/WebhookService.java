package io.websitecd.operator.webhook;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
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
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class WebhookService {

    private static final Logger log = Logger.getLogger(WebhookService.class);

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_IGNORED = "IGNORED";
    public static final String STATUS_UPDATING = "UPDATING";

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
        GitWebHookManager manager = getManager(request);
        if (manager == null) return Future.failedFuture(new BadRequestException("unknown provider"));

        JsonObject resultObject = new JsonObject().put("status", STATUS_SUCCESS);

        List<Website> websites;
        boolean someWebsitesUpdated;
        try {
            manager.validateRequest(request, data);

            websites = manager.getAuthorizedWebsites(request, data);

            JsonArray updatedSites = handleWebsites(websites);
            resultObject.put("websites", updatedSites);
            someWebsitesUpdated = updatedSites.size() != 0;
        } catch (Exception e) {
            return Future.failedFuture(e);
        }

        String gitUrl = manager.getGitUrl(data);
        String ref = manager.getRef(data);
        List<Future> updates = operatorService.updateRelatedComponents(gitUrl, ref, websites);
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

    protected JsonArray handleWebsites(List<Website> websites) throws GitAPIException, IOException {
        JsonArray updatedSites = new JsonArray();

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
