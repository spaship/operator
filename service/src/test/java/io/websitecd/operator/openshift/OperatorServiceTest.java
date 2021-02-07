package io.websitecd.operator.openshift;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.websitecd.operator.rest.GitlabWebhookTestCommon;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
public class OperatorServiceTest extends GitlabWebhookTestCommon {


    @Test
    public void testSimpleExample() throws Exception {
        registerSimpleWeb();
    }


    @Test
    public void testAdvancedExample() throws Exception {
        registerAdvancedWeb();
    }

}