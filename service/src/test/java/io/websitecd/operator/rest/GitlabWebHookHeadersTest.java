package io.websitecd.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class GitlabWebHookHeadersTest extends WebhookTestCommon {

    @Test
    public void testGitUrlMissing() {
        String body = new JsonObject()
                .put("ref", "someRef")
                .toString();

        givenSimpleGitlabWebhookRequest()
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

        givenSimpleGitlabWebhookRequest()
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(is("Unsupported Event"));
    }

}