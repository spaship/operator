package io.websitecd.operator.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.restassured.http.ContentType;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookGitUrlUnknownTest extends GitlabWebhookTestCommon {

    @Test
    public void ignoredGitUrl() throws Exception {
        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookGitUrlUnknownTest.class.getResourceAsStream("/gitlab-push-giturl-unknown.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("IGNORED"));
    }

}