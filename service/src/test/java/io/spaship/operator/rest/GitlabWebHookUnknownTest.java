package io.spaship.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class GitlabWebHookUnknownTest extends WebhookTestCommon {

    @Test
    public void unknownGitUrlInComponents() throws Exception {
        registerSimpleWeb();

        String body = getGitlabEventBody("UNKNOWN", "main");

        givenSimpleGitlabWebhookRequest()
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(is("no matched website"));
    }

    @Test
    public void unknownGitBranchInComponents() throws Exception {
        registerSimpleWeb();

        String body = getGitlabEventBody(SIMPLE_WEB.getGitUrl(), "UNKNOWN");

        givenSimpleGitlabWebhookRequest()
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("websites.size()", is(1))
                .body("websites[0].name", is("simple")).body("websites[0].status", is("IGNORED"))
                .body("components.size()", is(0));  // no matched component

    }

}