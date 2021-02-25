package io.websitecd.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
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

}
