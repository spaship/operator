package io.websitecd.operator.controller;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteList;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.websiteconfig.GitWebsiteConfigService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class WebsiteController {

    private static final Logger log = Logger.getLogger(WebsiteController.class);

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    OperatorService operatorService;

    @Inject
    ContentController contentController;

    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;

    @Inject
    WebsiteRepository websiteRepository;

    @ConfigProperty(name = "app.operator.provider.crd.enabled")
    boolean crdEnabled;

    @ConfigProperty(name = "app.operator.provider.crd.watch.resyncPeriodSec")
    int resyncPeriodSec;

    @Inject
    Vertx vertx;

    private boolean ready = false;

    void onStart(@Observes StartupEvent ev) {
        if (!crdEnabled) {
            ready = true;
            return;
        }
        initWebsiteCrd();
    }

    public void initWebsiteCrd() {
        log.infof("CRD enabled. Going to register CRD watch");
        watch();
    }

    public void watch() {
        SharedInformerFactory sharedInformerFactory = client.informers();
        SharedIndexInformer<Website> podInformer = sharedInformerFactory.sharedIndexInformerFor(
                Website.class, WebsiteList.class, TimeUnit.SECONDS.toMillis(resyncPeriodSec));

        podInformer.addEventHandler(new ResourceEventHandler<>() {
            @Override
            public void onAdd(Website website) {
                // TODO Check if resources are successfully deployed and anything is needed to be redeployed
                websiteAdded(website);
            }

            @Override
            public void onUpdate(Website oldWebsite, Website newWebsite) {
                if (oldWebsite.getMetadata().getResourceVersion().equals(newWebsite.getMetadata().getResourceVersion())) {
                    return;
                }
                websiteModified(newWebsite);
            }

            @Override
            public void onDelete(Website website, boolean deletedFinalStateUnknown) {
                websiteDeleted(website);
            }
        });
        sharedInformerFactory.startAllRegisteredInformers();

        ready = true;
    }

    public void websiteAdded(Website website) {
        log.infof("Website added, websiteId=%s", website.getId());

        try {
            WebsiteConfig config = gitWebsiteConfigService.cloneRepo(website);
            website.setConfig(config);

            websiteRepository.addWebsite(website);
            operatorService.initNewWebsite(website);
        } catch (Exception e) {
            log.error("Error on CRD added", e);
            throw new RuntimeException(e);
        }
    }

    public void websiteModified(Website newWebsite) {
        log.infof("Website modified, websiteId=%s", newWebsite.getId());

        try {
            Website oldWebsite = websiteRepository.getWebsite(newWebsite.getId());
            WebsiteConfig newConfig;
            if (websiteSpecGitChanged(oldWebsite.getSpec(), newWebsite.getSpec())) {
                log.infof("Spec changed. Refreshing setup");
                gitWebsiteConfigService.deleteRepo(oldWebsite);
                newConfig = gitWebsiteConfigService.cloneRepo(newWebsite);
            } else {
                newConfig = gitWebsiteConfigService.updateRepo(newWebsite);
            }
            boolean configChanged = !newConfig.equals(oldWebsite.getConfig());
            if (configChanged) {
                newWebsite.setConfig(newConfig);
                websiteRepository.addWebsite(newWebsite);
                operatorService.initInfrastructure(newWebsite, true);
            }
        } catch (Exception e) {
            log.error("Error on CRD modified", e);
            throw new RuntimeException(e);
        }
    }

    public static boolean websiteSpecGitChanged(WebsiteSpec oldSpec, WebsiteSpec newSpec) {
        return (!StringUtils.equals(oldSpec.getGitUrl(), newSpec.getGitUrl())
                || !StringUtils.equals(oldSpec.getBranch(), newSpec.getBranch())
                || !StringUtils.equals(oldSpec.getDir(), newSpec.getDir()));
    }

    public void websiteDeleted(Website websiteToDelete) {
        log.infof("Website deleted, websiteId=%s", websiteToDelete.getId());
        try {
            Website website = websiteRepository.getWebsite(websiteToDelete.getId());
            if (website != null) {
                gitWebsiteConfigService.deleteRepo(websiteToDelete);
                operatorService.deleteInfrastructure(website);
                websiteRepository.removeWebsite(website.getId());
            }
        } catch (Exception e) {
            log.error("Error on CRD deleted", e);
            throw new RuntimeException(e);
        }
    }

    public boolean isReady() {
        return ready;
    }

}
