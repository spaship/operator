package io.websitecd.operator.router;

import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.websitecd.operator.QuarkusTestBase;
import io.websitecd.operator.config.OperatorConfigUtils;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class RouterControllerTest extends QuarkusTestBase {

    @Inject
    RouterController controller;

    @Override
    protected void setupMockServer(KubernetesMockServer mockServer) {
        mockServer.expect()
                .post().withPath("/apis/route.openshift.io/v1/namespaces/websitecd-examples/routes")
                .andReturn(200, new IngressBuilder().build()).always();
    }

    @Test
    void testWebsiteRoutes() throws IOException {
        Website website = WebhookTestCommon.ADVANCED_WEBSITE;

        try (InputStream is = RouterControllerTest.class.getResourceAsStream("/advanced-website.yaml")) {
            WebsiteConfig config = OperatorConfigUtils.loadYaml(is);
            website.setConfig(config);
        }

        controller.updateWebsiteRoutes("dev", website);
    }

}