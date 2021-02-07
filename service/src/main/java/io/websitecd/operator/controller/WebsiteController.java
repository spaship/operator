package io.websitecd.operator.controller;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.CustomResource;
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
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.WebsiteConfigService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class WebsiteController {

    private static final Logger log = Logger.getLogger(WebsiteController.class);

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    OperatorService operatorService;

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    WebsiteRepository websiteRepository;

    @ConfigProperty(name = "app.operator.provider.crd.enabled")
    boolean crdEnabled;

    @Inject
    Vertx vertx;

    private boolean ready = false;

    void onStart(@Observes StartupEvent ev) throws IOException {
        if (!crdEnabled) {
            ready = true;
            return;
        }
        log.infof("CRD enabled. Going to register CRD");
        initWebsiteCrd();
    }

    public void initWebsiteCrd() throws IOException {
        registerCrd();
        watch();
        ready = true;
    }

    public void registerCrd() throws IOException {
        CustomResourceDefinitionList crds = client.apiextensions().v1().customResourceDefinitions().list();
        List<CustomResourceDefinition> crdsItems = crds.getItems();
        log.debugf("Found %s CRD(s)", crdsItems.size());
        CustomResourceDefinition websiteCRD = null;
        final String websiteCRDName = CustomResource.getCRDName(Website.class);
        for (CustomResourceDefinition crd : crdsItems) {
            ObjectMeta metadata = crd.getMetadata();
            if (metadata != null) {
                String name = metadata.getName();
                if (websiteCRDName.equals(name)) {
                    websiteCRD = crd;
                }
            }
        }
        if (websiteCRD != null) {
            log.infof("Found CRD: %s", websiteCRD.getMetadata());
        } else {
            try (InputStream is = WebsiteController.class.getResourceAsStream("/openshift/website-crd.yaml")) {
                websiteCRD = client.apiextensions().v1().customResourceDefinitions().load(is).get();
                client.apiextensions().v1().customResourceDefinitions().create(websiteCRD);
            }
            log.infof("Created CRD name=%s", websiteCRD.getMetadata().getName());
        }
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
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                log.error("onClose", cause);
            }
        });
    }

    public void websiteAdded(Website resource) {
        log.infof("Website added, resource=%s", resource);

        try {
            websiteRepository.addWebsite(resource);
            operatorService.initServices(resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void websiteModified(Website resource) {
        log.infof("Website modified, resource=%s", resource);
        websiteRepository.addWebsite(resource);

        WebsiteConfig oldConfig = websiteConfigService.getConfig(resource);
        try {
            WebsiteConfig newConfig = websiteConfigService.updateRepo(resource);
            if (WebsiteController.deploymentChanged(oldConfig, newConfig)) {
                operatorService.processConfig(resource, true, false);
            }
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
