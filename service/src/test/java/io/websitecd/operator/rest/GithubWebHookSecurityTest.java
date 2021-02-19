package io.websitecd.operator.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GithubWebHookSecurityTest extends WebhookTestCommon {

    @Test
    public void testUnauthenticated() {
        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "push")
                .body(GithubWebHookSecurityTest.class.getResourceAsStream("/github-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    public void testBadToken() throws Exception {
        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "Push Hook")
                .header("X-Hub-Signature-256", "BAD_TOKEN")
                .body(GithubWebHookSecurityTest.class.getResourceAsStream("/github-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

}
