package io.websitecd.operator.rest;

import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.vertx.core.Vertx;
import io.websitecd.operator.ContentApiMock;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookWebsiteChangeTest extends GitlabWebhookTestCommon {

    @Inject
    ContentController contentController;

    @Test
    public void gitPushWebsiteChange() throws Exception {
        ContentApiMock apiMock = new ContentApiMock(contentController.getStaticContentApiPort());

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(apiMock);

        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-prod")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();

        // TODO: Correctly return deployment
//        mockServer.expect()
//                .get().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev")
//                .andReturn(200, new DeploymentSpecBuilder().build()).always();
        // And test if redeploy happened

        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookWebsiteChangeTest.class.getResourceAsStream("/gitlab-push-website-changed.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("status", is("SUCCESS"))
                .body("website.name", is("simple"));

        assertEquals(0, apiMock.getApiListCount());
        assertEquals(0, apiMock.getApiUpdateTest1());
        assertEquals(0, apiMock.getApiUpdateTest2());

    }

}