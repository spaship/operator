package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.websitecd.operator.QuarkusTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ContentWatcherTest extends QuarkusTestBase {

    @Inject
    ContentWatcher contentWatcher;

    @BeforeEach
    protected void setupWatch() {
        mockServer.expect()
                .get().withPath("/apis/apps/v1/deployments")
                .andReturn(200, new DeploymentListBuilder().build())
                .always();
    }

    @Test
    void initWatcher() throws InterruptedException {
        contentWatcher.initWatcher(0);
        Thread.sleep(100);
        assertPathsRequested("/apis/apps/v1/deployments");
        contentWatcher.stopInformers();
    }

    @Test
    void isManagedByOperator() {
        assertTrue(contentWatcher.isManagedByOperator(new DeploymentBuilder().withMetadata(new ObjectMetaBuilder().withLabels(Map.of("managedBy", "websitecd-operator")).build()).build()));
    }

    @Test
    void replicasNotChanged() {
        assertTrue(contentWatcher.replicasNotChanged(new DeploymentStatusBuilder().withReplicas(0).build(), new DeploymentStatusBuilder().withReplicas(0).build()));
        assertTrue(contentWatcher.replicasNotChanged(new DeploymentStatusBuilder().withReadyReplicas(0).build(), new DeploymentStatusBuilder().withReadyReplicas(0).build()));
        assertFalse(contentWatcher.replicasNotChanged(new DeploymentStatusBuilder().withReplicas(0).build(), new DeploymentStatusBuilder().withReplicas(1).build()));
        assertFalse(contentWatcher.replicasNotChanged(new DeploymentStatusBuilder().withReadyReplicas(0).build(), new DeploymentStatusBuilder().withReadyReplicas(1).build()));
    }

    @Test
    void getStatusStr() {
        assertEquals("[0/0]", contentWatcher.getStatusStr(new DeploymentStatusBuilder().build()));
        assertEquals("[0/1]", contentWatcher.getStatusStr(new DeploymentStatusBuilder().withReadyReplicas(0).withReplicas(1).build()));
    }

}