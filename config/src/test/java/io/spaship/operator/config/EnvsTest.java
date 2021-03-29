package io.spaship.operator.config;

import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.spaship.operator.config.model.Environment;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EnvsTest {


    @Test
    public void testEmpty() throws IOException {
        assertNotNull(OperatorConfigUtils.getContentEnvsJson(""));
        assertNotNull(OperatorConfigUtils.getContentEnvsJson("{}"));
        assertNotNull(OperatorConfigUtils.getContentEnvsJson("{\"envs\": { } }"));
    }

    @Test
    public void testAll() throws IOException {
        try (InputStream is = OperatorConfigUtils.class.getResourceAsStream("/operator-envs-test.json")) {
            Map<String, Environment> envs = OperatorConfigUtils.getContentEnvsJson(is);

            assertEquals(2, envs.size());
            Environment dev = envs.get("dev");
            Environment prod = envs.get("prod");

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
}
