package io.spaship.operator.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.QuarkusTestBase;
import io.spaship.operator.crd.Website;
import io.spaship.operator.crd.WebsiteList;
import io.spaship.operator.crd.WebsiteStatus;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

@QuarkusTest
class WebsiteControllerTest extends QuarkusTestBase {

    @Inject
    WebsiteController websiteController;

    List<String> envs = List.of("dev", "prod");
    List<String> addedPaths;
    List<String> deletedPaths;

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

        addedPaths = expectedRegisterWebRequests(2);
        addedPaths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple");
        addedPaths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple/status");
        addedPaths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple");
        addedPaths.add("/apis/spaship.io/v1/namespaces/spaship-examples/websites/simple/status");

        deletedPaths = expectedDeleteWebRequests(envs, "spaship-examples", "simple");
    }

    @Test
    void websiteAddedAndDeleted() {
        Website website = WebhookTestCommon.SIMPLE_WEBSITE;

        websiteController.websiteAdded(website);
        assertPathsRequested(addedPaths);

        mockDeleted(website.getMetadata().getName(), envs);

        websiteController.websiteDeleted(website);
        assertPathsRequested(deletedPaths);

    }

    @Test
    void websiteAddedStatusFailed() {
        Website website = WebhookTestCommon.SIMPLE_WEBSITE;
        website.getStatus().setStatus(WebsiteStatus.STATUS.FAILED);

        websiteController.websiteAdded(website);
        assertPathsRequested(addedPaths);

        mockDeleted(website.getMetadata().getName(), envs);

        websiteController.websiteDeleted(website);
        assertPathsRequested(deletedPaths);
    }

}