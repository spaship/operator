package io.websitecd.operator.rest;

import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GithubWebHookStaticUpdateTest extends WebhookTestCommon {

    @Test
    public void gitPushStaticUpdate() throws Exception {
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-prod")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();

        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-GitHub-Event", "push")
                .header("X-Hub-Signature-256", OperatorServiceTest.SECRET_ADVANCED_SIGN)  // Different secret to not trigger website update
                .body(GithubWebHookStaticUpdateTest.class.getResourceAsStream("/github-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("websites.size()", is(0))
                .body("components.size()", is(2));

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateThemeCount());
        assertEquals(1, apiMock.getApiUpdateRootCount());
    }


}