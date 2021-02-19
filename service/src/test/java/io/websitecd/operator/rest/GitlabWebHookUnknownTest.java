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
class GitlabWebHookUnknownTest extends WebhookTestCommon {

    @Test
    public void unknownGitUrlInComponents() throws Exception {
        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookUnknownTest.class.getResourceAsStream("/gitlab-push-giturl-unknown.json"))
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

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookUnknownTest.class.getResourceAsStream("/gitlab-push-gitbranch-unknown.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body(is("no matched website or components"));
    }

}