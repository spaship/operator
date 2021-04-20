package io.spaship.operator;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.MockServer;
import io.quarkus.test.kubernetes.client.OpenShiftMockServerTestResource;
import okhttp3.mockwebserver.RecordedRequest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@QuarkusTest
@QuarkusTestResource(OpenShiftMockServerTestResource.class)
public class QuarkusTestBase {

    private static final Logger log = Logger.getLogger(QuarkusTestBase.class);

    public static final String EXAMPLES_NAMESPACE = "spaship-examples";

    @MockServer
    protected OpenShiftMockServer mockServer;

    long defaultWaitingTime = 100;

    @BeforeEach
    protected void setupMockServer() {
        mockServer.expect()
                .get().withPath("/apis/apiextensions.k8s.io/v1/customresourcedefinitions")
                .andReturn(200, new CustomResourceDefinitionBuilder().build())
                .always();

        setupMockServerDeployment("spaship-examples");

        mockServer.expect()
                .get().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-dev")
                .andReturn(200, new DeploymentBuilder().withMetadata(new ObjectMetaBuilder().withName("simple-content-dev").build()).build()).always();
        mockServer.expect()
                .get().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-prod")
                .andReturn(200, new DeploymentBuilder().withMetadata(new ObjectMetaBuilder().withName("simple-content-prod").build()).build()).always();

    }

    protected void setupMockServerDeployment(String namespace) {
        mockServer.expect()
                .post().withPath("/api/v1/namespaces/" + namespace + "/configmaps")
                .andReturn(200, new ConfigMapBuilder().build())
                .always();
        mockServer.expect()
                .post().withPath("/api/v1/namespaces/" + namespace + "/services")
                .andReturn(200, new ServiceSpecBuilder().build())
                .always();
        mockServer.expect()
                .post().withPath("/apis/apps/v1/namespaces/" + namespace + "/deployments")
                .andReturn(200, new DeploymentBuilder().build())
                .always();
    }

    protected void mockDeleted(String websiteName, List<String> envs) {
        for (String env : envs) {
            mockServer.expect()
                    .delete().withPath("/api/v1/namespaces/spaship-examples/services/" + websiteName + "-content-" + env)
                    .andReturn(200, null)
                    .always();
            mockServer.expect()
                    .delete().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/" + websiteName + "-content-" + env)
                    .andReturn(200, null)
                    .always();
            mockServer.expect()
                    .delete().withPath("/apis/extensions/v1beta1/namespaces/spaship-examples/deployments/" + websiteName + "-content-" + env)
                    .andReturn(200, null)
                    .always();
            mockServer.expect()
                    .delete().withPath("/api/v1/namespaces/spaship-examples/configmaps/" + websiteName + "-content-init-" + env)
                    .andReturn(200, null)
                    .always();
        }
    }

    protected void assertPathsRequested(String... paths) {
        final List<String> expectedPaths = new ArrayList<>(paths.length);
        Collections.addAll(expectedPaths, paths);

        assertPathsRequested(expectedPaths);
    }

    protected void assertPathsRequested(List<String> expectedPaths) {
        RecordedRequest request;
        try {
            while ((request = mockServer.takeRequest(defaultWaitingTime, TimeUnit.MILLISECONDS)) != null) {
                if (!expectedPaths.remove(request.getPath())) {
                    throw new AssertionError("Unknown path requested: " + request.getPath());
                }
            }

            if (!expectedPaths.isEmpty()) {
                throw new AssertionError("Expected paths not requested: " + expectedPaths);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Website meets all requested paths");
    }

    public static List<String> expectedRegisterWebRequests(int envsCount) {
        return expectedRegisterWebRequests(envsCount, EXAMPLES_NAMESPACE);
    }

    public static List<String> expectedRegisterWebRequests(int envsCount, String namespace) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < envsCount; i++) {
            result.add("/api/v1/namespaces/" + namespace + "/configmaps");
            result.add("/api/v1/namespaces/" + namespace + "/services");
            result.add("/apis/apps/v1/namespaces/" + namespace + "/deployments");
        }
        return result;
    }

    public static List<String> expectedDeleteWebRequests(List<String> envs, String namespace, String name) {
        ArrayList<String> result = new ArrayList<>();
        for (String env : envs) {
            result.add("/api/v1/namespaces/" + namespace + "/configmaps/" + name + "-content-init-" + env);
            result.add("/api/v1/namespaces/" + namespace + "/services/" + name + "-content-" + env);
            result.add("/apis/apps/v1/namespaces/" + namespace + "/deployments/" + name + "-content-" + env);
        }
        return result;
    }
}
