package io.websitecd.operator.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookSecurityTest extends GitlabWebhookTestCommon {

    @Test
    public void testUnauthenticated() throws Exception {
        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .body(GitlabWebHookSecurityTest.class.getResourceAsStream("/gitlab-push.json"))
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
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", "BAD_TOKEN")
                .body(GitlabWebHookSecurityTest.class.getResourceAsStream("/gitlab-push-website-changed.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

}
