package io.spaship.operator.controller;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.openshift.api.model.Route;
import io.quarkus.runtime.StartupEvent;
import io.spaship.content.git.config.GitContentUtils;
import io.spaship.operator.Utils;
import io.spaship.operator.config.model.ComponentConfig;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.content.ContentController;
import io.spaship.operator.content.UpdatedComponent;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteEnvs;
import io.spaship.operator.crd.WebsiteSpec;
import io.spaship.operator.crd.WebsiteStatus;
import io.spaship.operator.event.EventSourcingEngine;
import io.spaship.operator.router.IngressController;
import io.spaship.operator.router.RouterController;
import io.spaship.operator.utility.EventAttribute;
import io.spaship.operator.websiteconfig.GitWebsiteConfigService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.spaship.operator.config.matcher.ComponentKindMatcher.ComponentGitMatcher;

@ApplicationScoped
/**
 * TODO change the name to WebsiteService and make it accessible only by the WebsiteController
 */
public class OperatorService {

    private static final Logger log = Logger.getLogger(OperatorService.class);

    @Inject
    ContentController contentController;

    @Inject
    RouterController routerController;

    @Inject
    IngressController ingressController;

    @Inject
    WebsiteRepository websiteRepository;

    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;

    @Inject
    WebsiteController websiteController;

    @ConfigProperty(name = "app.content.git.rootcontext")
    protected String rootContext;

    @Inject
    Vertx vertx;

    @Inject
    EventSourcingEngine eventSourcingEngine; //TODO Preferably declare this variable final and use constructor injection,

    // eager start
    void startup(@Observes StartupEvent event) {
    }

    public WebsiteStatus initNewWebsite(Website website, boolean redeploy) {
        Set<String> enabledEnvs = website.getEnabledEnvs();
        log.infof("Init infrastructure for websiteId=%s, enabledEnvs=%s redeploy=%s", website.getId(), enabledEnvs, redeploy);
        websiteRepository.addWebsite(website);

        RuntimeException exception = null;
        WebsiteStatus status = website.getStatus();
        if (status == null) {
            status = new WebsiteStatus();
        }
        for (String env : enabledEnvs) {
            try {
                log.debugf("Processing env=%s", env);

                setupCoreServices(env, website);

                String apiHost = null;
                Integer port = null;
                if (ingressController.isEnabled()) {
                    Ingress ingress = ingressController.updateIngress(env, website);
                    if (ingress != null) {
                        String contentHost = ingress.getSpec().getRules().get(0).getHost();
                        status.addEnvHost(env, contentHost);
                    }
                } else if (routerController.isEnabled()) {
                    List<Route> contentRoutes = routerController.updateWebsiteRoutes(env, website);
                    if (contentRoutes != null && contentRoutes.size() > 0) {
                        String contentHost = contentRoutes.get(0).getSpec().getHost();
                        status.addEnvHost(env, contentHost);
                    }
                    routerController.updateApiRoute(env, website);
                } else {
                    log.infof("No routing created");
                }

                if (redeploy) {
                    contentController.redeploy(env, website);
                }
            } catch (RuntimeException ex) {
                log.error("Error processing env=" + env, ex);
                exception = ex;
                // continue with processing other environments and throw exception after loop ends
            }
        }
        if (exception != null) {
            throw exception;
        }

        //IMPLEMENTATION OF ISSUE 59 Start
        String eventPayload = Utils.buildEventPayload(EventAttribute.CR_NAME.concat(website.getMetadata().getName()),
                EventAttribute.NAMESPACE.concat(website.getMetadata().getNamespace()),
                EventAttribute.CODE.concat(EventAttribute.EventCode.WEBSITE_CREATE.name()),
                EventAttribute.MESSAGE.concat("website created")
        );
        eventSourcingEngine.publishMessage(eventPayload);
        //IMPLEMENTATION OF ISSUE 59 End

        log.debugf("Infrastructure initialized. status=%s", status);
        return status;
    }

    private void setupCoreServices(String env, Website website) {
        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);
        final String websiteInfo = String.format("websiteName=%s env=%s namespace=%s", websiteName, env, namespace);
        log.infof("Create core services %s", websiteInfo);
        contentController.updateConfigs(env, namespace, website);
        contentController.deploy(env, namespace, websiteName, website);
        website.getConfig().getEnabledGitComponents(env)
                .forEach(c -> log.infof("Deployed component for %s context=%s gitUrl=%s ref=%s",
                        websiteInfo, c.getContext(), c.getSpec().getUrl(), GitContentUtils.getRef(website.getConfig(), env, c.getContext())));
    }

    public void deleteInfrastructure(Website website) {
        log.infof("Delete infrastructure for websiteId=%s", website.getId());

        String namespace = website.getMetadata().getNamespace();
        final String websiteName = Utils.getWebsiteName(website);

        for (String env : website.getEnabledEnvs()) {
            contentController.deleteDeployment(env, namespace, websiteName);
            contentController.deleteConfigs(env, namespace, website);

            if (ingressController.isEnabled()) {
                ingressController.deleteIngress(env, website);
            } else if (routerController.isEnabled()) {
                routerController.deleteWebsiteRoutes(env, website);
            } else {
                log.infof("No routing deleted");
            }
        }
    }

    public List<Future> updateRelatedComponents(List<Website> authorizedWebsites, String gitUrl, String ref, Set<String> updatedWebsites) {
        List<Future> updates = new ArrayList<>();
        // Iterate over authorized websites
        for (Website website : authorizedWebsites) {
            if (updatedWebsites != null && updatedWebsites.contains(website.getId())) {
                continue;
            }
            for (ComponentConfig component : website.getConfig().getComponents()) {
                if (!ComponentGitMatcher.test(component) || !StringUtils.equals(gitUrl, component.getSpec().getUrl())) {
                    continue;
                }
                log.tracef("Component with same gitUrl found. context=%s", component.getContext());
                List<Future<UpdatedComponent>> componentsUpdates = website.getEnabledEnvs().stream()
                        .filter(env -> {
                            String componentRef = GitContentUtils.getRef(website.getConfig(), env, component.getContext());
                            return ref.endsWith(componentRef);
                        })
                        .map(env -> contentController.refreshComponent(website, env, GitContentUtils.getDirName(component.getContext(), rootContext)))
                        .collect(Collectors.toList());
                updates.addAll(componentsUpdates);
            }
        }
        return updates;
    }

    /**
     * Rollout website
     *
     * @param website
     * @param forceRollout if true new and old config is not checked
     * @return
     * @throws GitAPIException
     * @throws IOException
     *
     * TODO Should be accessed by WebsiteController only
     * clarification: without taking the WebsiteController in confidence this method is either deploying  of updating an
     * existing deployment , it would be difficult to Figure the entry point for controlling purpose.
     *
     */
    public boolean rolloutWebsiteNonBlocking(Website website, boolean forceRollout) throws GitAPIException, IOException {
        String websiteId = website.getId();

        WebsiteConfig newConfig = gitWebsiteConfigService.updateRepo(website);
        if (!forceRollout) {
            Website oldWebsite = websiteRepository.getWebsite(website.getId());
            if (newConfig.equals(oldWebsite.getConfig())) {
                log.infof("Configs are same for websiteId=%s. No rollout performed", websiteId);
                return false;
            }
        }

        vertx.executeBlocking(future -> {
            log.infof("Rollout websiteId=%s", websiteId);
            try {
                website.setConfig(newConfig);

                initNewWebsite(website, true);
                future.complete();
            } catch (Exception e) {
                future.fail(e.getMessage());
            }
        }, res -> {
            if (res.succeeded()) {
                log.infof("Website updated websiteId=%s", websiteId);

                //IMPLEMENTATION OF ISSUE 59 Start
                String eventPayload = Utils.buildEventPayload(EventAttribute.CR_NAME.concat(website.getMetadata().getName()),
                        EventAttribute.NAMESPACE.concat(website.getMetadata().getNamespace()),
                        EventAttribute.CODE.concat(EventAttribute.EventCode.WEBSITE_UPDATE.name()),
                        EventAttribute.MESSAGE.concat("website updated")
                );
                eventSourcingEngine.publishMessage(eventPayload);
                //IMPLEMENTATION OF ISSUE 59 End

            } else {
                log.error("Cannot update website, websiteId=" + websiteId, res.cause());
            }
        });
        return true;
    }

    public static Website createWebsiteCopy(Website website, String previewId, String previewGitUrl, String previewRef) {
        Website previewWebsite = new Website();

        WebsiteSpec sourceSpec = website.getSpec();
        log.tracef("source spec to copy=%s", sourceSpec);
        // Git Spec is from Merge request
        // For some reason Openshift reject "null" values even they're not required. That's why "trimToEmpty"
        WebsiteSpec spec = new WebsiteSpec(previewGitUrl, previewRef, StringUtils.trimToEmpty(sourceSpec.getDir()),
                sourceSpec.getSslVerify(), StringUtils.trimToEmpty(sourceSpec.getSecretToken()));
        spec.setPreviews(false);
        spec.setDisplayName(StringUtils.trimToEmpty(sourceSpec.getDisplayName()) + " - Fork");
        spec.setEnvs(sourceSpec.getEnvs() != null ? sourceSpec.getEnvs() : new WebsiteEnvs());
        if (spec.getEnvs().getIncluded() == null) spec.getEnvs().setIncluded(new ArrayList<>());
        if (spec.getEnvs().getExcluded() == null) spec.getEnvs().setExcluded(new ArrayList<>());

        previewWebsite.setSpec(spec);

        // Just change the name to "name"-<previewId>
        ObjectMeta sourceMetadata = website.getMetadata();
        String previewName = sourceMetadata.getName() + "-pr-" + previewId;

        ObjectMetaBuilder metadata = new ObjectMetaBuilder()
                .withName(previewName).withNamespace(sourceMetadata.getNamespace())
                .addToLabels("websiteFork", sourceMetadata.getName());

        previewWebsite.setMetadata(metadata.build());
        return previewWebsite;
    }

    public void createOrUpdateWebsite(Website website, boolean redeploy) throws GitAPIException, IOException {
        log.infof("Create/Update website website_id=%s redeploy=%s", website.getId(), redeploy);

        if (!websiteController.isCrdEnabled()) {
            deployNewWebsite(website, true, redeploy);
            return;
        }

        Website existingWebsite = websiteController.getWebsiteClient()
                .inNamespace(website.getMetadata().getNamespace())
                .withName(website.getMetadata().getName()).get();

        if (existingWebsite != null) {
            websiteController.updateStatus(existingWebsite, WebsiteStatus.STATUS.FORCE_UPDATE);

            //IMPLEMENTATION OF ISSUE 59 Start
            String eventPayload = Utils.buildEventPayload(EventAttribute.CR_NAME.concat(website.getMetadata().getName()),
                    EventAttribute.NAMESPACE.concat(website.getMetadata().getNamespace()),
                    EventAttribute.CODE.concat(EventAttribute.EventCode.PREVIEW_UPDATE.name()),
                    EventAttribute.MESSAGE.concat("website preview update done")
            );
            eventSourcingEngine.publishMessage(eventPayload);
            //IMPLEMENTATION OF ISSUE 59 End

        } else { // This is basically creation of new website
            websiteController.getWebsiteClient()
                    .inNamespace(website.getMetadata().getNamespace())
                    .withName(website.getMetadata().getName())
                    .createOrReplace(website);

            //IMPLEMENTATION OF ISSUE 59 Start
            String eventPayload = Utils.buildEventPayload(EventAttribute.CR_NAME.concat(website.getMetadata().getName()),
                    EventAttribute.NAMESPACE.concat(website.getMetadata().getNamespace()),
                    EventAttribute.CODE.concat(EventAttribute.EventCode.PREVIEW_CREATE.name()),
                    EventAttribute.MESSAGE.concat("website preview update done")
            );
            eventSourcingEngine.publishMessage(eventPayload);
            //IMPLEMENTATION OF ISSUE 59 End

        }
    }

    public void deleteWebsite(Website website) {
        log.infof("Deleting website website_id=%s", website.getId());

        if (websiteController.isCrdEnabled()) {
            websiteController.getWebsiteClient().inNamespace(website.getMetadata().getNamespace()).delete(website);
        } else {
            Website websiteToDelete = websiteRepository.getWebsite(website.getId());
            deleteInfrastructure(websiteToDelete);
        }
        //IMPLEMENTATION OF ISSUE 59 Start
        String eventPayload = Utils.buildEventPayload(EventAttribute.CR_NAME.concat(website.getMetadata().getName()),
                EventAttribute.NAMESPACE.concat(website.getMetadata().getNamespace()),
                EventAttribute.CODE.concat(EventAttribute.EventCode.PREVIEW_DELETE.name()),
                EventAttribute.MESSAGE.concat("website preview deleted")
        );
        eventSourcingEngine.publishMessage(eventPayload);
        //IMPLEMENTATION OF ISSUE 59 End
    }

    public WebsiteStatus deployNewWebsite(Website website, boolean updateGitIfExists, boolean redeploy) throws IOException, GitAPIException {
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(website, updateGitIfExists);
        website.setConfig(websiteConfig);
        return initNewWebsite(website, redeploy);
    }

    public void updateAndRegisterWebsite(Website website, boolean updateGitIfExists) throws GitAPIException, IOException {
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(website, updateGitIfExists);
        website.setConfig(websiteConfig);
        websiteRepository.addWebsite(website);
    }

}
