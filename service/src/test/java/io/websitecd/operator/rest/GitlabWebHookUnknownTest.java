package io.websitecd.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GitlabWebHookUnknownTest extends WebhookTestCommon {

    @Test
    public void unknownGitUrlInComponents() throws Exception {
        registerSimpleWeb();

        String body = getGitlabEventBody("UNKNOWN", "main");

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", "Avoid rollout update")
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body(is("no matched website or components"));
    }

    @Test
    public void unknownGitBranchInComponents() throws Exception {
        registerSimpleWeb();

        String body = getGitlabEventBody(SIMPLE_WEB.getGitUrl(), "UNKNOWN");

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", "Avoid rollout update")
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body(is("no matched website or components"));
    }

}