package io.websitecd.operator.openshift;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
public class OperatorServiceTest {


    @MockServer
    KubernetesMockServer mockServer;

    @Inject
    OperatorService operatorService;

    @Inject
    WebsiteConfigService websiteConfigService;

    public static final String GIT_EXAMPLES_URL = "https://github.com/websitecd/websitecd-examples.git";
    public static final String GIT_EXAMPLES_BRANCH = "main";
    public static final String GIT_EXAMPLES_CONFIG_ADVANCED = "websites/02-advanced";
    public static final String GIT_EXAMPLES_CONFIG_SIMPLE = "websites/01-simple";

    @Test
    public void testSimpleExample() throws Exception {
        setupMockServer(mockServer);
        websiteConfigService.setConfigDir(Optional.of(GIT_EXAMPLES_CONFIG_SIMPLE));
        operatorService.initServices(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH);
    }


    @Test
    public void testAdvancedExample() throws Exception {
        setupMockServer(mockServer);
        websiteConfigService.setConfigDir(Optional.of(GIT_EXAMPLES_CONFIG_ADVANCED));
        operatorService.initServices(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH);
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