package io.websitecd.operator.config;

import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.websitecd.TestUtils;
import io.websitecd.operator.config.model.Environment;
import io.websitecd.operator.config.model.WebsiteConfig;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DeploymentConfigTest {

    private String testedFile = "/deployment-config-test.yaml";

    @Test
    public void testDeployment() throws IOException {
        WebsiteConfig config = TestUtils.loadConfig(testedFile);

        assertEquals(3, config.getEnvs().size());
        Environment dev = config.getEnvironment("dev");
        Environment stage = config.getEnvironment("stage");
        Environment prod = config.getEnvironment("prod");

        assertEquals(Integer.valueOf(1), dev.getDeployment().getReplicas());
        assertEquals(Integer.valueOf(5), prod.getDeployment().getReplicas());

        ResourceRequirements devInitResources = dev.getDeployment().getInit().getResources();
        assertEquals("100", devInitResources.getRequests().get("cpu").getAmount());
        assertEquals("110", devInitResources.getRequests().get("memory").getAmount());
        assertEquals("120", devInitResources.getLimits().get("cpu").getAmount());
        assertEquals("130", devInitResources.getLimits().get("memory").getAmount());

        ResourceRequirements devHttpdResources = dev.getDeployment().getHttpd().getResources();
        assertEquals("140", devHttpdResources.getRequests().get("cpu").getAmount());
        assertEquals("150", devHttpdResources.getRequests().get("memory").getAmount());
        assertEquals("160", devHttpdResources.getLimits().get("cpu").getAmount());
        assertEquals("170", devHttpdResources.getLimits().get("memory").getAmount());

        ResourceRequirements devApiResources = dev.getDeployment().getApi().getResources();
        assertEquals("180", devApiResources.getRequests().get("cpu").getAmount());
        assertEquals("190", devApiResources.getRequests().get("memory").getAmount());
        assertEquals("200", devApiResources.getLimits().get("cpu").getAmount());
        assertEquals("210", devApiResources.getLimits().get("memory").getAmount());

    }

}
