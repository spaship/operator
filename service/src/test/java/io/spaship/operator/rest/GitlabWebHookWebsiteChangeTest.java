package io.spaship.operator.rest;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.crd.Website;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GitlabWebHookWebsiteChangeTest extends WebhookTestCommon {

    @Test
    public void gitPushWebsiteConfigChange() throws Exception {
        registerSimpleWeb();

        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-dev")
                .andReturn(200, new DeploymentBuilder().withMetadata(new ObjectMetaBuilder().withName("simple-content-dev").build()).build()).always();
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-prod")
                .andReturn(200, new DeploymentBuilder().withMetadata(new ObjectMetaBuilder().withName("simple-content-prod").build()).build()).always();

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
                .body("websites[0].status", is("UPDATING"))
                .body("components.size()", is(0));

        List<String> requests = expectedRegisterWebRequests(2);
        requests.add("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-prod");
        requests.add("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-prod");
        requests.add("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-dev");
        requests.add("/apis/apps/v1/namespaces/spaship-examples/deployments/simple-content-dev");

        assertPathsRequested(requests);

        assertEquals(0, apiMock.getApiListCount());
        assertEquals(0, apiMock.getApiUpdateThemeCount());
        assertEquals(0, apiMock.getApiUpdateRootCount());
    }

    @Test
    public void gitPushWebsiteNoChangeJustComponentUpdate() throws Exception {
        registerSimpleWeb();

        givenSimpleGitlabWebhookRequest()
                .body(GitlabWebHookWebsiteChangeTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple"))
                .body("websites[0].status", is("IGNORED"))
                .body("components.size()", is(2));
    }

}