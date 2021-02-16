package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.websitecd.operator.controller.WebsiteController;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ContentWatcher {

    private static final Logger log = Logger.getLogger(ContentWatcher.class);

    @Inject
    DefaultOpenShiftClient client;

    @Inject
    WebsiteController websiteController;

    @ConfigProperty(name = "app.operator.provider.crd.enabled")
    boolean crdEnabled;

    private long resyncPeriodSec = 60;

    void onStart(@Observes StartupEvent ev) {
        if (!crdEnabled) {
            return;
        }
        initWatcher();
    }

    private void initWatcher() {
        log.info("Registering ContentWatcher");

        SharedInformerFactory sharedInformerFactory = client.informers();
        SharedIndexInformer<Deployment> deploymentInformer = sharedInformerFactory.sharedIndexInformerFor(Deployment.class, DeploymentList.class, TimeUnit.SECONDS.toMillis(resyncPeriodSec));
        deploymentInformer.addEventHandler(new ResourceEventHandler<>() {
            @Override
            public void onAdd(Deployment resource) {
                if (!isManagedByOperator(resource)) return;

                deploymentUpdated(resource);
            }

            @Override
            public void onUpdate(Deployment oldResource, Deployment newResource) {
                if (!isManagedByOperator(newResource)) return;
                if (oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion()))
                    return;

                deploymentUpdated(newResource);
            }

            @Override
            public void onDelete(Deployment resource, boolean deletedFinalStateUnknown) {
            }
        });
        sharedInformerFactory.startAllRegisteredInformers();
    }

    public boolean isManagedByOperator(Deployment deployment) {
        return deployment.getMetadata().getLabels() != null
                && deployment.getMetadata().getLabels().containsKey("managedBy")
                && StringUtils.equals(deployment.getMetadata().getLabels().get("managedBy"), "websitecd-operator");
    }

    private void deploymentUpdated(Deployment deployment) {
        try {
            String websiteName = deployment.getMetadata().getLabels().get("website");
            String env = deployment.getMetadata().getLabels().get("env");
            DeploymentStatus status = deployment.getStatus();
            String statusStr = String.format("[%s/%s]",
                    defaultZero(status.getReadyReplicas()),
                    defaultZero(status.getReplicas()));
            websiteController.updateStatusEnv(deployment.getMetadata().getNamespace(), websiteName, env, statusStr);
        } catch (Exception e) {
            log.error("Error on Updating status env", e);
            throw new RuntimeException(e);
        }
    }
    public String defaultZero(Number n) {
        if (n == null) {
            return "0";
        }
        return n.toString();
    }


}
