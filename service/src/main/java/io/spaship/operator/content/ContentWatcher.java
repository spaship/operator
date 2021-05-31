package io.spaship.operator.content;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.quarkus.runtime.StartupEvent;
import io.spaship.operator.controller.WebsiteController;
import io.spaship.operator.crd.Website;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ContentWatcher {

    private static final Logger log = Logger.getLogger(ContentWatcher.class);
    private final long resyncPeriodSec = 60;
    @Inject
    DefaultOpenShiftClient client;
    @Inject
    WebsiteController websiteController;
    @ConfigProperty(name = "app.operator.provider.crd.enabled")
    boolean crdEnabled;
    @Inject
    Vertx vertx;

    void onStart(@Observes StartupEvent ev) {
        log.infof("ContentWatcher enabled=%s", crdEnabled);
        if (!crdEnabled) {
            return;
        }
        initWatcher(100);
    }

    protected void initWatcher(int delay) {
        SharedInformerFactory sharedInformerFactory = client.informers();
        SharedIndexInformer<Deployment> deploymentInformer = sharedInformerFactory.sharedIndexInformerFor(Deployment.class, TimeUnit.SECONDS.toMillis(resyncPeriodSec));
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
                if (replicasNotChanged(oldResource.getStatus(), newResource.getStatus())) return;

                deploymentUpdated(newResource);
            }

            @Override
            public void onDelete(Deployment deployment, boolean deletedFinalStateUnknown) {
                if (!isManagedByOperator(deployment)) return;
                String websiteName = deployment.getMetadata().getLabels().get("website");
                String websiteId = Website.createId(deployment.getMetadata().getNamespace(), websiteName);
                log.infof("Deployment deleted. websiteId=%s deletedFinalStateUnknown=%s", websiteId, deletedFinalStateUnknown);
            }
        });
        // slightly wait
        if (delay > 0) {
            vertx.setTimer(delay, value -> sharedInformerFactory.startAllRegisteredInformers());
        } else {
            sharedInformerFactory.startAllRegisteredInformers();
        }
    }

    protected void stopInformers() {
        client.informers().stopAllRegisteredInformers();
    }

    public boolean isManagedByOperator(Deployment deployment) {
        return deployment.getMetadata().getLabels() != null
                && deployment.getMetadata().getLabels().containsKey("managedBy")
                && StringUtils.equals(deployment.getMetadata().getLabels().get("managedBy"), "spaship-operator");
    }

    public boolean replicasNotChanged(DeploymentStatus oldStatus, DeploymentStatus newStatus) {
        return oldStatus != null && newStatus != null &&
                Objects.equals(newStatus.getReplicas(), oldStatus.getReplicas()) &&
                Objects.equals(newStatus.getReadyReplicas(), oldStatus.getReadyReplicas());
    }

    private void deploymentUpdated(Deployment deployment) {
        try {
            String websiteName = deployment.getMetadata().getLabels().get("website");
            String env = deployment.getMetadata().getLabels().get("env");
            String statusStr = getStatusStr(deployment.getStatus());
            websiteController.updateStatusEnv(deployment.getMetadata().getNamespace(), websiteName, env, statusStr);
        } catch (Exception e) {
            log.error("Error on Updating status env", e);
            throw new RuntimeException(e);
        }
    }

    protected String getStatusStr(DeploymentStatus status) {
        return String.format("[%s/%s]",
                defaultZero(status.getReadyReplicas()),
                defaultZero(status.getReplicas()));
    }

    public String defaultZero(Number n) {
        if (n == null) {
            return "0";
        }
        return n.toString();
    }

}
