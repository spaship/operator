package io.spaship.operator.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GitlabWebHookSecurityTest extends WebhookTestCommon {

    @Test
    public void testUnauthenticated() {
        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .body(GitlabWebHookSecurityTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(is("X-Gitlab-Token missing"));
    }

    @Test
    public void testBadToken() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb();

        String body = getGitlabEventBody(SIMPLE_WEB.getGitUrl(), SIMPLE_WEB.getBranch());

        givenSimpleGitlabWebhookRequest()
                .header("X-Gitlab-Token", "bad token")
                .body(body)
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(401)
                .body(is("no matched website"));
    }

}
