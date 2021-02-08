package io.websitecd.operator.controller;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteList;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.openshift.GitWebsiteConfigService;
import io.websitecd.operator.openshift.OperatorService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class WebsiteController {

    private static final Logger log = Logger.getLogger(WebsiteController.class);

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    OperatorService operatorService;

    @Inject
    GitWebsiteConfigService gitWebsiteConfigService;

    @Inject
    WebsiteRepository websiteRepository;

    @ConfigProperty(name = "app.operator.provider.crd.enabled")
    boolean crdEnabled;

    @Inject
    Vertx vertx;

    private boolean ready = false;

    void onStart(@Observes StartupEvent ev) {
        if (!crdEnabled) {
            ready = true;
            return;
        }
        log.infof("CRD enabled. Going to register CRD");
        initWebsiteCrd();
    }

    public void initWebsiteCrd() {
        watch();
    }

    public void watch() {
        NonNamespaceOperation<Website, WebsiteList, Resource<Website>> websiteClient = client.inAnyNamespace().customResources(Website.class, WebsiteList.class);

        websiteClient.watch(new Watcher<>() {
            @Override
            public void eventReceived(Watcher.Action action, Website resource) {
                log.debugf("==> %s for %s", action, resource);
                WebsiteSpec websiteSpec = resource.getSpec();
                if (websiteSpec == null) {
                    log.errorf("No Spec for resource=%s", resource);
                    return;
                }
                switch (action) {
                    case ADDED:
                        // TODO Check if resources are succesfully deployed and anything is needed to be redeployed
                        websiteAdded(resource);
                        break;
                    case MODIFIED:
                        websiteModified(resource);
                        break;
                    case DELETED:
                        websiteDeleted(resource);
                        break;
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                log.error("onClose", cause);
                ready = false;
            }
        });
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
            throw new RuntimeException(e);
        }
    }

    public void websiteModified(Website website) {
        log.infof("Website modified, websiteId=%s", website.getId());

        try {
            Website oldWebsite = websiteRepository.getWebsite(website.getId());
            WebsiteConfig newConfig;
            if (websiteSpecChanged(oldWebsite.getSpec(), website.getSpec())) {
                log.infof("Spec changed. Refreshing setup");
                gitWebsiteConfigService.deleteRepo(oldWebsite);
                newConfig = gitWebsiteConfigService.cloneRepo(website);
            } else {
                newConfig = gitWebsiteConfigService.updateRepo(website);
            }
            website.setConfig(newConfig);
            if (WebsiteController.deploymentChanged(oldWebsite.getConfig(), newConfig)) {
                websiteRepository.addWebsite(website);
                operatorService.initInfrastructure(website, true, false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean websiteSpecChanged(WebsiteSpec oldSpec, WebsiteSpec newSpec) {
        return (!StringUtils.equals(oldSpec.getGitUrl(), newSpec.getGitUrl())
                || !StringUtils.equals(oldSpec.getBranch(), newSpec.getBranch())
                || !StringUtils.equals(oldSpec.getDir(), newSpec.getDir()));
    }

    public void websiteDeleted(Website website) {
        log.infof("Website deleted, websiteId=%s", website.getId());
        try {
            WebsiteConfig newConfig = gitWebsiteConfigService.updateRepo(website);
            website.setConfig(newConfig);
            websiteRepository.removeWebsite(website);
            operatorService.deleteInfrastructure(website);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean deploymentChanged(WebsiteConfig oldConfig, WebsiteConfig newConfig) {
        // TODO: Compare old and new config and consider if deployment has changed
        return true;
    }

    public boolean isReady() {
        return ready;
    }

}
