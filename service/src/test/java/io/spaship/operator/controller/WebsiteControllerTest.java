package io.spaship.operator.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.QuarkusTestBase;
import io.spaship.operator.crd.WebsiteList;
import io.spaship.operator.rest.WebhookTestCommon;
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
                .get().withPath("/apis/spaship.io/v1/websites")
                .andReturn(200, websiteList)
                .always();
        mockServer.expect()
                .get().withPath("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        mockServer.expect()
                .put().withPath("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple/status")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        mockServer.expect()
                .get().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-dev")
                .andReturn(200, WebhookTestCommon.SIMPLE_WEBSITE)
                .always();
        mockServer.expect()
                .get().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-prod")
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
        paths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple");
        paths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple/status");
        paths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple");
        paths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple/status");

        assertPathsRequested(paths);

        websiteController.websiteDeleted(WebhookTestCommon.SIMPLE_WEBSITE);
        assertPathsRequested(
                "/api/v1/namespaces/spaship-examples/services/simple-content-prod",
                "/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-prod",
                "/api/v1/namespaces/spaship-examples/configmaps/simple-content-init-prod",
                "/api/v1/namespaces/spaship-examples/services/simple-content-dev",
                "/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-dev",
                "/api/v1/namespaces/spaship-examples/configmaps/simple-content-init-dev"
        );

    }

}