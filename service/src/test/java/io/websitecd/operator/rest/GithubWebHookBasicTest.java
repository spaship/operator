package io.websitecd.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GithubWebHookBasicTest extends WebhookTestCommon {

    @Test
    public void testEmptyBody() {
        String body = new JsonObject().toString();
        given()
                .header("Content-type", "application/json")
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(is("Body is empty"));
    }

    @Test
    public void testUnknownProvider() {
        given()
                .header("Content-type", "application/json")
                .body(GithubWebHookStaticUpdateTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(is("unknown provider"));
    }

    @Test
    public void testGitUrlMissing() {
        String body = new JsonObject()
                .put("ref", "someRef")
                .toString();
        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "push")
                .header("X-Hub-Signature-256", OperatorServiceTest.SECRET_SIMPLE_SIGN)
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(is("Unsupported Event"));
    }

    @Test
    public void testGitBranchMissing() {
        String body = new JsonObject()
                .put("repository", new JsonObject().put("clone_url", "git url"))
                .toString();

        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "push")
                .header("X-Hub-Signature-256", OperatorServiceTest.SECRET_SIMPLE_SIGN)
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(is("Unsupported Event"));
    }

    @Test
    public void githubPing() throws Exception {
        // Test of Github Ping
        // https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#ping
        given()
                .header("Content-type", "application/json")
                .body(GithubWebHookStaticUpdateTest.class.getResourceAsStream("/github-ping.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("PING"));

    }

}