package io.websitecd.operator.controller;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.crd.WebsiteList;
import io.websitecd.operator.crd.WebsiteSpec;
import io.websitecd.operator.crd.WebsiteStatus;
import io.websitecd.operator.crd.WebsiteStatus.STATUS;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.websiteconfig.GitWebsiteConfigService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    @ConfigProperty(name = "app.operator.provider.crd.watch.resyncPeriodSec")
    int resyncPeriodSec;

    @Inject
    Vertx vertx;

    private boolean ready = false;

    MixedOperation<Website, WebsiteList, Resource<Website>> websiteClient;

    void onStart(@Observes StartupEvent ev) {
        if (!crdEnabled) {
            ready = true;
            return;
        }
        initWebsiteCrd();
    }

    public void initWebsiteCrd() {
        log.infof("CRD enabled. Going to register CRD watch");
        websiteClient = client.customResources(Website.class, WebsiteList.class);
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
                if (oldWebsite.getSpec().equals(newWebsite.getSpec())) {
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
        log.infof("Website added, websiteId=%s status=%s", website.getId(), website.getStatus());

        Website websiteCrd;
        websiteCrd = updateStatus(website, STATUS.GIT_CLONING);
        try {
            WebsiteConfig config = gitWebsiteConfigService.cloneRepo(website);
            website.setConfig(config);

            websiteRepository.addWebsite(website);

            websiteCrd = updateStatus(websiteCrd, STATUS.CREATING);
            Set<String> envs = operatorService.initNewWebsite(website);

            websiteCrd.getStatus().setEnvs(envs.toString());
            websiteCrd.getStatus().setStatus(STATUS.DONE);
            updateStatus(websiteCrd);
        } catch (Exception e) {
            log.error("Error on CRD added", e);
            websiteCrd.getStatus().setMessage(e.getMessage());
            websiteCrd.getStatus().setStatus(STATUS.FAILED);
            updateStatus(websiteCrd);
            throw new RuntimeException(e);
        }
    }

    public void websiteModified(Website newWebsite) {
        log.infof("Website modified, websiteId=%s", newWebsite.getId());

        Website websiteCrd = newWebsite;
        try {
            Website oldWebsite = websiteRepository.getWebsite(newWebsite.getId());
            WebsiteConfig newConfig;
            if (websiteSpecGitChanged(oldWebsite.getSpec(), newWebsite.getSpec())) {
                log.infof("Spec changed. Refreshing setup");
                gitWebsiteConfigService.deleteRepo(oldWebsite);
                websiteCrd = updateStatus(websiteCrd, STATUS.GIT_CLONING);
                newConfig = gitWebsiteConfigService.cloneRepo(newWebsite);
            } else {
                websiteCrd = updateStatus(websiteCrd, STATUS.GIT_PULLING);
                newConfig = gitWebsiteConfigService.updateRepo(newWebsite);
            }
            newWebsite.setConfig(newConfig);
            websiteRepository.addWebsite(newWebsite);
            websiteCrd = updateStatus(websiteCrd, STATUS.UPDATING);
            Set<String> envs = operatorService.initInfrastructure(newWebsite, true);
            websiteCrd.getStatus().setEnvs(envs.toString());
            websiteCrd.getStatus().setStatus(STATUS.DONE);
            updateStatus(websiteCrd);
        } catch (Exception e) {
            log.error("Error on CRD modified", e);
            websiteCrd.getStatus().setMessage(e.getMessage());
            websiteCrd.getStatus().setStatus(STATUS.FAILED);
            updateStatus(websiteCrd);
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

    public Website updateStatus(Website website, STATUS newStatus) {
        log.infof("Update Status, websiteId=%s status=%s", website.getId(), newStatus);
        if (website.getStatus() == null) {
            website.setStatus(new WebsiteStatus());
        }

        if (newStatus.compareTo(STATUS.FAILED) != 0) {
            website.getStatus().setMessage("");
        }
        website.getStatus().setStatus(newStatus);

        return updateStatus(website);
    }

    public Website updateStatus(Website website) {
        return websiteClient.inNamespace(website.getMetadata().getNamespace()).withName(website.getMetadata().getName()).updateStatus(website);
    }

    public boolean isReady() {
        return ready;
    }

}
