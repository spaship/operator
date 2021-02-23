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
import io.websitecd.operator.websiteconfig.GitWebsiteConfigService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        SharedIndexInformer<Website> websiteInformer = sharedInformerFactory.sharedIndexInformerFor(Website.class, TimeUnit.SECONDS.toMillis(resyncPeriodSec));

        websiteInformer.addEventHandler(new ResourceEventHandler<>() {
            @Override
            public void onAdd(Website website) {
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

            updateStatus(websiteCrd, STATUS.DEPLOYED);
            operatorService.initNewWebsite(website);
        } catch (Exception e) {
            log.error("Error on CRD added", e);
            updateStatus(websiteCrd, STATUS.FAILED, e.getMessage());
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

            updateStatus(websiteCrd, STATUS.DEPLOYED);
            operatorService.initInfrastructure(newWebsite, true);
        } catch (Exception e) {
            log.error("Error on CRD modified", e);
            updateStatus(websiteCrd, STATUS.FAILED, e.getMessage());
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
            removeLock(websiteToDelete.getMetadata().getNamespace(), websiteToDelete.getMetadata().getName());
        } catch (Exception e) {
            log.error("Error on CRD deleted", e);
            throw new RuntimeException(e);
        }
    }

    public Website updateStatus(Website websiteToUpdate, STATUS newStatus) {
        return updateStatus(websiteToUpdate, newStatus, "");
    }

    public Website updateStatus(Website websiteToUpdate, STATUS newStatus, String message) {
        String ns = websiteToUpdate.getMetadata().getNamespace();
        String name = websiteToUpdate.getMetadata().getName();
        final String lock = getLock(ns, name);
        synchronized (lock) {
            Website website = websiteClient.inNamespace(ns).withName(name).get();

            log.infof("Update Status, websiteId=%s status=%s", website.getId(), newStatus);
            WebsiteStatus status = website.getStatus();
            if (status == null) {
                status = new WebsiteStatus("", "", new ArrayList<>());
            }
            if (status.getEnvs() == null) {
                status.setEnvs(new ArrayList<>());
            }
            if (message != null) {
                status.setMessage(message);
            }
            status.setStatus(newStatus);
            website.setStatus(status);

            try {
                return websiteClient.inNamespace(ns).withName(name).updateStatus(website);
            } catch (Exception e) {
                log.warn("Cannot update status", e);
                return website;
            }
        }
    }

    /**
     * Update status for given environment.
     *
     * @param namespace
     * @param name
     * @param envName
     * @param value
     */
    public void updateStatusEnv(String namespace, String name, String envName, String value) {
        final String lock = getLock(namespace, name);
        synchronized (lock) {
            Website website = websiteClient.inNamespace(namespace).withName(name).get();
            if (website == null) return;

            log.infof("Website updated env for websiteId=%s envName=%s value=%s", website.getId(), envName, value);

            List<String> envs = website.getStatus().getEnvs();
            String envValue = envName + value;
            int existingIndex = -1;
            if (envs == null) {
                envs = new ArrayList<>();
            } else {
                for (int i = 0; i < envs.size(); i++) {
                    if (envs.get(i).startsWith(envName)) {
                        existingIndex = i;
                        break;
                    }
                }
            }
            if (existingIndex != -1) {
                envs.set(existingIndex, envValue);
            } else {
                envs.add(envValue);
            }


            website.getStatus().setEnvs(envs);
            websiteClient.inNamespace(namespace).withName(name).updateStatus(website);
        }
    }


    /**
     * Simple lock for each website.
     */
    private Map<String, String> locks = new HashMap<>();

    private String getLock(String ns, String name) {
        final String id = lockId(ns, name);
        if (locks.containsKey(id)) {
            return locks.get(id);
        } else {
            locks.put(id, id);
            return id;
        }
    }

    private synchronized void removeLock(String ns, String name) {
        locks.remove(lockId(ns, name));
    }

    private String lockId(String ns, String name) {
        return Website.createId(ns, name);
    }

    public boolean isReady() {
        return ready;
    }

}
