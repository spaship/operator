package io.spaship.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.spaship.operator.crd.Website;
import io.spaship.operator.openshift.OperatorServiceTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GithubWebHookMergeRequestTest extends WebhookTestCommon {

    @Test
    public void testMergeDataMissing() {
        String body = new JsonObject()
                .put("repository", new JsonObject().put("clone_url", "git url"))
                .toString();

        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "pull_request")
                .header("X-Hub-Signature-256", OperatorServiceTest.SECRET_SIMPLE_SIGN)
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(is("Merge Event Data missing"));
    }

    @Test
    public void mergeRequestOpen() throws Exception {
        Website website = SIMPLE_WEBSITE;
        website.getSpec().setPreviews(true);
        registerWeb(website, true);

        setupMockServerRedeploy(List.of("dev", "prod"), EXAMPLES_NAMESPACE, "simple-pr-1");

        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "pull_request")
                .header("X-Hub-Signature-256", OperatorServiceTest.SECRET_SIMPLE_SIGN)
                .body(GithubWebHookMergeRequestTest.class.getResourceAsStream("/github-merge-request.json"))
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

}
