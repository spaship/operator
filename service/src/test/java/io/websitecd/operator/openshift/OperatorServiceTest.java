package io.websitecd.operator.openshift;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import io.websitecd.operator.rest.GitlabWebhookTestCommon;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
public class OperatorServiceTest extends GitlabWebhookTestCommon {


    @MockServer
    KubernetesMockServer mockServer;


    @Test
    public void testSimpleExample() throws Exception {
        setupMockServer(mockServer);
        registerSimpleWeb();
    }


    @Test
    public void testAdvancedExample() throws Exception {
        setupMockServer(mockServer);
        registerAdvancedWeb();
    }

    public static void setupMockServer(KubernetesMockServer mockServer) throws Exception {
        mockServer.expect()
                .post().withPath("/api/v1/namespaces/websitecd-examples/configmaps")
                .andReturn(200, new ConfigMapBuilder().build())
                .always();

        mockServer.expect()
                .post().withPath("/api/v1/namespaces/websitecd-examples/services")
                .andReturn(200, new ServiceSpecBuilder().build())
                .always();

        mockServer.expect()
                .post().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments")
                .andReturn(200, new DeploymentSpecBuilder().build())
                .always();
    }

}