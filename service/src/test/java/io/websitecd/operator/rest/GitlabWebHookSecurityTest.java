package io.websitecd.operator.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.vertx.core.Vertx;
import io.websitecd.operator.ContentApiMock;
import io.websitecd.operator.content.ContentController;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookSecurityTest extends GitlabWebhookTestCommon {

    @Inject
    ContentController contentController;

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

        ContentApiMock apiMock = new ContentApiMock(contentController.getStaticContentApiPort());
        apiMock.reset();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(apiMock);

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", "BAD_TOKEN")
                .body(GitlabWebHookSecurityTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(400);

        apiMock.reset();
        vertx.close();
    }

}
