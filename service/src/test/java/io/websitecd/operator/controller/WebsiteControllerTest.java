package io.websitecd.operator.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.websitecd.operator.QuarkusTestBase;
import io.websitecd.operator.crd.WebsiteList;
import io.websitecd.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

@QuarkusTest
class WebsiteControllerTest extends QuarkusTestBase {

    @Inject
    WebsiteController websiteController;

    @BeforeEach
    protected void setupCrdMock() {
        WebsiteList websiteList = new WebsiteList();
        websiteList.setItems(List.of(WebhookTestCommon.SIMPLE_WEBSITE));
        mockServer.expect()
                .get().withPath("/apis/websitecd.io/v1/websites")
                .andReturn(200, websiteList)
                .always();
        mockServer.expect()
                .get().withPath("/apis/websitecd.io/v1/namespaces/websitecd-examples/websites/simple")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        mockServer.expect()
                .put().withPath("/apis/websitecd.io/v1/namespaces/websitecd-examples/websites/simple/status")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        mockServer.expect()
                .get().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        mockServer.expect()
                .get().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-prod")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        websiteController.initWebsiteCrd();
    }

    @Test
    void websiteAddedAndDeleted() {
        mockDeleted("simple", "dev");
        mockDeleted("simple", "prod");

        websiteController.websiteAdded(WebhookTestCommon.SIMPLE_WEBSITE);

        List<String> paths = expectedRegisterWebRequests(2);
        paths.add("/apis/websitecd.io/v1/namespaces/websitecd-examples/websites/simple");
        paths.add("/apis/websitecd.io/v1/namespaces/websitecd-examples/websites/simple/status");
        paths.add("/apis/websitecd.io/v1/namespaces/websitecd-examples/websites/simple");
        paths.add("/apis/websitecd.io/v1/namespaces/websitecd-examples/websites/simple/status");

        assertPathsRequested(paths);

        websiteController.websiteDeleted(WebhookTestCommon.SIMPLE_WEBSITE);
        assertPathsRequested(
                "/api/v1/namespaces/websitecd-examples/services/simple-content-prod",
                "/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-prod",
                "/api/v1/namespaces/websitecd-examples/configmaps/simple-content-init-prod",
                "/api/v1/namespaces/websitecd-examples/services/simple-content-dev",
                "/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev",
                "/api/v1/namespaces/websitecd-examples/configmaps/simple-content-init-dev"
        );

    }

}