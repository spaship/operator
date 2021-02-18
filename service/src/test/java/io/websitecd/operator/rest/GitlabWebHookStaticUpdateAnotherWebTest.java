package io.websitecd.operator.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.restassured.http.ContentType;
import io.websitecd.operator.ContentApiMock;
import io.websitecd.operator.config.model.WebsiteConfig;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.crd.Website;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookStaticUpdateAnotherWebTest extends GitlabWebhookTestCommon {

    @Inject
    ContentController contentController;

    @Test
    public void gitPushStaticUpdateJustComponent() throws Exception {
        Website website = SIMPLE_WEBSITE;

        websiteRepository.reset();
        WebsiteConfig websiteConfig = gitWebsiteConfigService.cloneRepo(website);
        websiteConfig.getComponents().get(0).getSpec().setUrl("COMPONENT_URL");
        websiteConfig.getComponents().get(1).getSpec().setUrl("COMPONENT2_URL");

        website.setConfig(websiteConfig);
        websiteRepository.addWebsite(website);
        operatorService.initNewWebsite(website);

        ContentApiMock apiMock = new ContentApiMock(contentController.getStaticContentApiPort());
        apiMock.reset();

        vertx.deployVerticle(apiMock);

        // Pushing component with different secret. It's VALID !!!
        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_ADVANCED)
                .body(GitlabWebHookStaticUpdateAnotherWebTest.class.getResourceAsStream("/gitlab-push-giturl-justcomponent.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("components[0].name", is("theme"));

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookStaticUpdateAnotherWebTest.class.getResourceAsStream("/gitlab-push-giturl-justcomponent.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("components[0].name", is("theme"));

        assertEquals(0, apiMock.getApiListCount());
        assertEquals(2, apiMock.getApiUpdateThemeCount());
        assertEquals(0, apiMock.getApiUpdateRootCount());

        apiMock.reset();
    }

}
