package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class ContentControllerTest {

    @Inject
    ContentController contentController;

    static String testedConfig = "/deployment-config-test.yaml";

    static WebsiteConfig config;

    @BeforeAll
    public static void beforeAll() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream(testedConfig)) {
            config = OperatorConfigUtils.loadYaml(is);
        }
    }

    @Test
    void overrideDevResources() throws IOException {
        String env = "dev";
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", "testwebsite");
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(true));

        KubernetesList kubernetesList = contentController.processTemplate("ns-1", params);
        Deployment deployment = getDeployment(kubernetesList);

        DeploymentSpec newDeploymentSpec = contentController.overrideDeployment(deployment, config.getEnvironment(env).getDeployment()).getSpec();

        Assert.assertEquals(Integer.valueOf(1), newDeploymentSpec.getReplicas());
    }

    @Test
    void overrideProdResources() throws IOException {
        String env = "prod";
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", "testwebsite");
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(true));

        KubernetesList kubernetesList = contentController.processTemplate("ns-1", params);
        Deployment deployment = getDeployment(kubernetesList);

        DeploymentSpec newDeploymentSpec = contentController.overrideDeployment(deployment, config.getEnvironment(env).getDeployment()).getSpec();

        Assert.assertEquals(Integer.valueOf(5), newDeploymentSpec.getReplicas());

        ResourceRequirements initResources = newDeploymentSpec.getTemplate().getSpec().getInitContainers().get(0).getResources();
        Assert.assertEquals("105", initResources.getRequests().get("cpu").getAmount());
        Assert.assertEquals("110", initResources.getRequests().get("memory").getAmount());
        Assert.assertEquals("120", initResources.getLimits().get("cpu").getAmount());
        Assert.assertEquals("130", initResources.getLimits().get("memory").getAmount());

        ResourceRequirements httpdResources = newDeploymentSpec.getTemplate().getSpec().getContainers().get(0).getResources();
        Assert.assertEquals("140", httpdResources.getRequests().get("cpu").getAmount());
        Assert.assertEquals("150", httpdResources.getRequests().get("memory").getAmount());
        Assert.assertEquals("160", httpdResources.getLimits().get("cpu").getAmount());
        Assert.assertEquals("170", httpdResources.getLimits().get("memory").getAmount());

        ResourceRequirements apiResources = newDeploymentSpec.getTemplate().getSpec().getContainers().get(1).getResources();
        Assert.assertEquals("180", apiResources.getRequests().get("cpu").getAmount());
        Assert.assertEquals("190", apiResources.getRequests().get("memory").getAmount());
        Assert.assertEquals("200", apiResources.getLimits().get("cpu").getAmount());
        Assert.assertEquals("210", apiResources.getLimits().get("memory").getAmount());
    }

    private Deployment getDeployment(KubernetesList list) {
        for (HasMetadata item : list.getItems()) {
            if (item instanceof Deployment) {
                return (Deployment) item;
            }
        }
        return null;
    }

}