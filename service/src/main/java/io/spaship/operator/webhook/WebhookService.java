package io.spaship.operator.webhook;

import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.controller.OperatorService;
import io.spaship.operator.crd.Website;
import io.spaship.operator.webhook.github.GithubWebHookManager;
import io.spaship.operator.webhook.gitlab.GitlabWebHookManager;
import io.spaship.operator.webhook.model.UpdatedWebsite;
import io.spaship.operator.webhook.model.WebhookResponse;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.spaship.operator.webhook.model.UpdatedWebsite.STATUS_IGNORED;
import static io.spaship.operator.webhook.model.UpdatedWebsite.STATUS_UPDATING;
import static io.spaship.operator.webhook.model.WebhookResponse.STATUS_PING;
import static io.spaship.operator.webhook.model.WebhookResponse.STATUS_SUCCESS;

@ApplicationScoped
public class WebhookService {

    private static final Logger log = Logger.getLogger(WebhookService.class);


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

    public Future<WebhookResponse> handleRequest(HttpServerRequest request, JsonObject data) {
        WebhookResponse resultObject = new WebhookResponse();

        GitWebHookManager manager = getManager(request);
        if (manager == null) {
            if (githubWebHookManager.isPingRequest(data)) {
                log.infof("Ping event registered. gitUrl=%s", githubWebHookManager.getGitUrl(request, data));
                resultObject.setStatus(STATUS_PING);
                return Future.succeededFuture(resultObject);
            } else {
                return Future.failedFuture(new BadRequestException("unknown provider"));
            }
        }

        resultObject.setStatus(STATUS_SUCCESS);

        List<UpdatedWebsite> updatedSites;
        boolean someWebsitesUpdated;
        List<Website> authorizedWebsites;
        try {
            manager.validateRequest(request, data);

            authorizedWebsites = manager.getAuthorizedWebsites(request, data);

            log.infof("Webhook call event=%s websites=%s", manager.getEventHeader(request), authorizedWebsites.size());
            if (authorizedWebsites.size() == 0) {
                return Future.failedFuture(new NotAuthorizedException("no matched website", "token"));
            }

            if (manager.isMergeRequest(request)) {
                updatedSites = handlePreview(authorizedWebsites, manager.getPreviewId(data), manager.getPreviewGitUrl(data), manager.getPreviewRef(data), manager.getMergeStatus(data));
                resultObject.setWebsites(updatedSites);
                resultObject.setComponents(new ArrayList<>());
                return Future.succeededFuture(resultObject);
            }

            updatedSites = handleWebsites(authorizedWebsites);
            resultObject.setWebsites(updatedSites);
            someWebsitesUpdated = updatedSites.size() != 0;
        } catch (Exception e) {
            return Future.failedFuture(e);
        }

        Set<String> updatedWebsiteIds = updatedSites.stream().
                filter(site -> !STATUS_IGNORED.equals(site.getStatus()))
                .map(site -> site.getNamespace() + "-" + site.getName())
                .collect(Collectors.toSet());

        final String gitUrl = manager.getGitUrl(request, data);
        String ref = manager.getRef(data);
        List<Future> updates = operatorService.updateRelatedComponents(authorizedWebsites, gitUrl, ref, updatedWebsiteIds);
        log.infof("Update components with same gitUrl and branch. gitUrl=%s ref=%s matchedComponents=%s", gitUrl, ref, updates.size());
        if (updates.size() == 0 && !someWebsitesUpdated) {
            return Future.failedFuture(new BadRequestException("no matched website or components"));
        }

        Promise<WebhookResponse> promise = Promise.promise();
        CompositeFuture.join(updates)
                .onFailure(promise::fail)
                .onSuccess(e -> {
                    if (e.result().list() != null) {
                        resultObject.setComponents(e.result().list());
                    }
                    promise.complete(resultObject);
                });

        return promise.future();
    }

    protected List<UpdatedWebsite> handleWebsites(List<Website> websites) throws GitAPIException, IOException {
        List<UpdatedWebsite> updatedSites = new ArrayList<>();

        for (Website website : websites) {
            boolean updatePerformed = operatorService.rolloutWebsiteNonBlocking(website, false);
            UpdatedWebsite result = new UpdatedWebsite(
                    website.getMetadata().getName(),
                    website.getMetadata().getNamespace(),
                    updatePerformed ? STATUS_UPDATING : STATUS_IGNORED);
            updatedSites.add(result);
        }
        return updatedSites;
    }

    protected List<UpdatedWebsite> handlePreview(List<Website> websites, String previewId, String previewGitUrl, String previewRef, GitWebHookManager.MergeStatus mergeStatus) throws GitAPIException, IOException {
        List<UpdatedWebsite> updatedSites = new ArrayList<>();

        for (Website website : websites) {
            UpdatedWebsite result;
            if (website.getSpec().getPreviews()) {
                Website websiteCopy = OperatorService.createWebsiteCopy(website, previewId, previewGitUrl, previewRef);
                if (mergeStatus == GitWebHookManager.MergeStatus.CLOSE) {
                    operatorService.deleteWebsite(websiteCopy);
                } else {
                    operatorService.createOrUpdateWebsite(websiteCopy, true);
                }
                result = new UpdatedWebsite(website.getMetadata().getName(), website.getMetadata().getNamespace(), mergeStatus.toResponseStatus());
            } else {
                result = new UpdatedWebsite(website.getMetadata().getName(), website.getMetadata().getNamespace(), STATUS_IGNORED);
                log.debugf("previews are disabled for website=%s", website.getMetadata().getName());
            }
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
