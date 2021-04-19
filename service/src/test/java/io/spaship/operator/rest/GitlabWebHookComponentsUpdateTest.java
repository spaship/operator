package io.spaship.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.spaship.operator.config.model.WebsiteConfig;
import io.spaship.operator.crd.Website;
import io.spaship.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GitlabWebHookComponentsUpdateTest extends WebhookTestCommon {

    @Test
    public void gitPushStaticUpdate() throws Exception {
        registerSimpleWeb();

        givenSimpleGitlabWebhookRequest()
                .body(GitlabWebHookComponentsUpdateTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple")).body("websites[0].status", is("IGNORED"))
                .body("components.size()", is(2));

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateThemeCount());
        assertEquals(1, apiMock.getApiUpdateRootCount());
    }

    @Test
    public void gitPushStaticUpdateAdvanced() throws Exception {
        registerAdvancedWeb();

        givenAdvancedGitlabWebhookRequest()
                .body(GitlabWebHookComponentsUpdateTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(1))
                .body("websites[0].name", is("advanced")).body("websites[0].status", is("IGNORED"))
                .body("components.size()", is(3));  // template, search, _root

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateThemeCount());
        assertEquals(1, apiMock.getApiUpdateSearchCount());
        assertEquals(1, apiMock.getApiUpdateRootCount());
        assertEquals(0, apiMock.getApiUpdateSharedCount());
    }

    @Test
    public void gitPushStaticUpdateAdvancedTag() throws Exception {
        registerAdvancedWeb();

        givenAdvancedGitlabWebhookRequest()
                .body(GitlabWebHookComponentsUpdateTest.class.getResourceAsStream("/gitlab-push-tag.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(1))
                .body("websites[0].name", is("advanced")).body("websites[0].status", is("IGNORED"))
                .body("components.size()", is(1))
                .body("components[0].name", is("shared-components"));

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateSharedCount());
        assertEquals(0, apiMock.getApiUpdateThemeCount());
        assertEquals(0, apiMock.getApiUpdateSearchCount());
        assertEquals(0, apiMock.getApiUpdateRootCount());
    }

    @Test
    public void gitPushStaticUpdateJustComponent() throws Exception {
        Website website = SIMPLE_WEBSITE;

        websiteRepository.reset();
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(website, false);
        websiteConfig.getComponents().get(0).getSpec().setUrl("COMPONENT_URL");
        websiteConfig.getComponents().get(1).getSpec().setUrl("COMPONENT2_URL");

        website.setConfig(websiteConfig);
        operatorService.initNewWebsite(website);

        String body = getGitlabEventBody("COMPONENT_URL", "main");

        // Pushing VALID component with different secret is INVALID
        givenSimpleGitlabWebhookRequest()
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_ADVANCED)
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(is("no matched website"));

        givenSimpleGitlabWebhookRequest()
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(is("no matched website"));

        assertEquals(0, apiMock.getApiListCount());
        assertEquals(0, apiMock.getApiUpdateThemeCount());
        assertEquals(0, apiMock.getApiUpdateRootCount());

        assertPathsRequested(expectedRegisterWebRequests(2));
    }

}