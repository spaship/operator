package io.websitecd.operator.openshift;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import io.websitecd.operator.crd.WebsiteSpec;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

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
    public static final String GIT_EXAMPLES_CONFIG_SIMPLE = "websites/01-simple";
    public static final String GIT_EXAMPLES_CONFIG_ADVANCED = "websites/02-advanced";
    public static final String SECRET = "testsecret";

    public static WebsiteSpec SIMPLE_WEB = new WebsiteSpec(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH, GIT_EXAMPLES_CONFIG_SIMPLE, true, SECRET);
    public static WebsiteSpec ADVANCED_WEB = new WebsiteSpec(GIT_EXAMPLES_URL, GIT_EXAMPLES_BRANCH, GIT_EXAMPLES_CONFIG_ADVANCED, true, SECRET);

    @Test
    public void testSimpleExample() throws Exception {
        setupMockServer(mockServer);
        operatorService.initServices(SIMPLE_WEB);
    }


    @Test
    public void testAdvancedExample() throws Exception {
        setupMockServer(mockServer);
        operatorService.initServices(ADVANCED_WEB);
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