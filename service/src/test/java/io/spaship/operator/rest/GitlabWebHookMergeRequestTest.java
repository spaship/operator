package io.spaship.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.spaship.operator.controller.OperatorService;
import io.spaship.operator.crd.Website;
import io.spaship.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GitlabWebHookMergeRequestTest extends WebhookTestCommon {

    @Test
    public void mergeRequestOpen() throws Exception {
        Website website = SIMPLE_WEBSITE;
        website.getSpec().setPreviews(true);
        registerWeb(website, true);

        setupMockServerRedeploy(List.of("dev", "prod"), EXAMPLES_NAMESPACE, "simple-pr-1");

        // CRD is disabled
//        websiteController.initWebsiteCrd();
//        mockServer.expect()
//                .post().withPath("/apis/spaship.io/v1/namespaces/spaship-examples/websites")
//                .andReturn(200, new Website())
//                .always();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Merge Request Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookMergeRequestTest.class.getResourceAsStream("/gitlab-merge-request.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple")).body("websites[0].status", is("PREVIEW_CREATING"))
                .body("components.size()", is(0));  // no matched component

        List<String> paths = expectedRegisterWebRequests(2, website.getMetadata().getNamespace());
        paths.addAll(expectedRedeployWebRequests(List.of("dev", "prod"), EXAMPLES_NAMESPACE, "simple-pr-1"));

        assertPathsRequested(paths);
    }

    @Test
    public void mergeRequestUpdate() throws Exception {
        Website website = SIMPLE_WEBSITE;
        website.getSpec().setPreviews(true);
        registerWeb(website, true);

        setupMockServerRedeploy(List.of("dev", "prod"), EXAMPLES_NAMESPACE, "simple-pr-1");

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Merge Request Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookMergeRequestTest.class.getResourceAsStream("/gitlab-merge-request-update.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple")).body("websites[0].status", is("PREVIEW_UPDATING"))
                .body("components.size()", is(0));  // no matched component

        List<String> paths = expectedRegisterWebRequests(2, website.getMetadata().getNamespace());
        paths.addAll(expectedRedeployWebRequests(List.of("dev", "prod"), EXAMPLES_NAMESPACE, "simple-pr-1"));

        assertPathsRequested(paths);
    }

    @Test
    public void mergeRequestClose() throws Exception {
        Website website = SIMPLE_WEBSITE;
        website.getMetadata().setName("simple");
        website.getSpec().setPreviews(true);
        registerWeb(website, true);

        Website websitePreview = OperatorService.createWebsiteCopy(website, "1",
                "https://github.com/spaship/spaship-examples.git", "integration-test");
        registerWeb(websitePreview, false);

        mockDeleted(websitePreview.getMetadata().getName(), List.of("dev", "prod"));

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Merge Request Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookMergeRequestTest.class.getResourceAsStream("/gitlab-merge-request-close.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("websites", hasSize(2))
                .body("websites[0].name", is("simple-pr-1"))
                .body("websites[0].status", is("IGNORED"))
                .body("websites[1].name", is("simple"))
                .body("websites[1].status", is("PREVIEW_DELETING"))
                .body("components", hasSize(0));  // no matched component

        assertPathsRequested(expectedDeleteWebRequests(List.of("dev", "prod"), websitePreview.getMetadata().getNamespace(), websitePreview.getMetadata().getName()));
    }

}