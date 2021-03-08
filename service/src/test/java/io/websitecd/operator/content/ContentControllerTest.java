package io.websitecd.operator.content;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.quarkus.test.junit.QuarkusTest;
import io.websitecd.operator.QuarkusTestBase;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ContentControllerTest extends QuarkusTestBase {

    @Inject
    ContentController contentController;

    static final String testedConfig = "/deployment-config-test.yaml";

    static WebsiteConfig config;

    @BeforeAll
    public static void beforeAll() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream(testedConfig)) {
            config = OperatorConfigUtils.loadYaml(is);
        }
    }

    @Test
    void overrideDevResources() {
        String env = "dev";
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", "testwebsite");
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(true));

        KubernetesList kubernetesList = contentController.processTemplate("ns-1", params);
        Deployment deployment = getDeployment(kubernetesList);

        DeploymentSpec newDeploymentSpec = contentController.overrideDeployment(deployment, config.getEnvironment(env).getDeployment()).getSpec();

        assertEquals(Integer.valueOf(1), newDeploymentSpec.getReplicas());
    }

    @Test
    void overrideProdResources() {
        String env = "prod";
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", "testwebsite");
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(true));

        KubernetesList kubernetesList = contentController.processTemplate("ns-1", params);
        Deployment deployment = getDeployment(kubernetesList);

        DeploymentSpec newDeploymentSpec = contentController.overrideDeployment(deployment, config.getEnvironment(env).getDeployment()).getSpec();

        assertEquals(Integer.valueOf(5), newDeploymentSpec.getReplicas());

        ResourceRequirements initResources = newDeploymentSpec.getTemplate().getSpec().getInitContainers().get(0).getResources();
        assertEquals("105", initResources.getRequests().get("cpu").getAmount());
        assertEquals("110", initResources.getRequests().get("memory").getAmount());
        assertEquals("120", initResources.getLimits().get("cpu").getAmount());
        assertEquals("130", initResources.getLimits().get("memory").getAmount());

        ResourceRequirements httpdResources = newDeploymentSpec.getTemplate().getSpec().getContainers().get(0).getResources();
        assertEquals("140", httpdResources.getRequests().get("cpu").getAmount());
        assertEquals("150", httpdResources.getRequests().get("memory").getAmount());
        assertEquals("160", httpdResources.getLimits().get("cpu").getAmount());
        assertEquals("170", httpdResources.getLimits().get("memory").getAmount());

        ResourceRequirements apiResources = newDeploymentSpec.getTemplate().getSpec().getContainers().get(1).getResources();
        assertEquals("180", apiResources.getRequests().get("cpu").getAmount());
        assertEquals("190", apiResources.getRequests().get("memory").getAmount());
        assertEquals("200", apiResources.getLimits().get("cpu").getAmount());
        assertEquals("210", apiResources.getLimits().get("memory").getAmount());
    }

    @Test
    void overrideImages() {
        String env = "dev";
        Map<String, String> params = new HashMap<>();
        params.put("ENV", env);
        params.put("NAME", "testwebsite");
        params.put("GIT_SSL_NO_VERIFY", Boolean.toString(true));
        params.put("IMAGE_INIT", "imageInit");
        params.put("IMAGE_HTTPD", "imageHttpd");
        params.put("IMAGE_API", "imageApi");
        params.put("IMAGE_INIT_VERSION", "imageInitVersion");
        params.put("IMAGE_HTTPD_VERSION", "imageHttpdVersion");
        params.put("IMAGE_API_VERSION", "imageApiVersion");

        KubernetesList kubernetesList = contentController.processTemplate("ns-1", params);
        Deployment deployment = getDeployment(kubernetesList);

        DeploymentSpec spec = deployment.getSpec();

        Container initContainer = spec.getTemplate().getSpec().getInitContainers().get(0);
        Container httpdContainer = spec.getTemplate().getSpec().getContainers().get(0);
        Container apiContainer = spec.getTemplate().getSpec().getContainers().get(1);

        assertEquals("imageInit:imageInitVersion", initContainer.getImage());
        assertEquals("imageHttpd:imageHttpdVersion", httpdContainer.getImage());
        assertEquals("imageApi:imageApiVersion", apiContainer.getImage());
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