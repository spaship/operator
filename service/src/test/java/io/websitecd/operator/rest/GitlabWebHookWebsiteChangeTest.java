package io.websitecd.operator.rest;

import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.websitecd.operator.crd.Website;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookWebsiteChangeTest extends WebhookTestCommon {

    @BeforeEach
    public void updateMock() throws Exception {
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-prod")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();
        registerSimpleWeb();

    }

    @Test
    public void gitPushWebsiteChangeNoConfigChange() throws Exception {
        // change local config to simulate website rollout
        Website localWebsite = SIMPLE_WEBSITE;
        localWebsite.getConfig().setLabels(Map.of("newLabel", "newLabelValue"));

        websiteRepository.addWebsite(localWebsite);

        givenSimpleGitlabWebhookRequest()
                .body(GitlabWebHookWebsiteChangeTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple"))
                .body("websites[0].status", is("UPDATING"));

        // Wait little bit till non blocking rollout finish
        Thread.sleep(100);

        assertEquals(0, apiMock.getApiListCount());
        assertEquals(0, apiMock.getApiUpdateThemeCount());
        assertEquals(0, apiMock.getApiUpdateRootCount());
    }

    @Test
    public void gitPushWebsiteNoChange() {
        givenSimpleGitlabWebhookRequest()
                .body(GitlabWebHookWebsiteChangeTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple"))
                .body("websites[0].status", is("IGNORED"));
    }

}