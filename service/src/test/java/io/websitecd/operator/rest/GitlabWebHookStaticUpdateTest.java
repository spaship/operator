package io.websitecd.operator.rest;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import io.restassured.http.ContentType;
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
@TestHTTPEndpoint(WebHookResource.class)
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookStaticUpdateTest extends GitlabWebhookTestCommon {

    @MockServer
    KubernetesMockServer mockServer;

    @Inject
    ContentController contentController;

    @Test
    public void gitPushStaticUpdate() throws Exception {
        ContentApiMock apiMock = new ContentApiMock(contentController.getStaticContentApiPort());
        apiMock.reset();

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(apiMock);

        OperatorServiceTest.setupMockServer(mockServer);

        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookStaticUpdateTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("components.size()", is(4));

        assertEquals(2, apiMock.getApiListCount());
        assertEquals(2, apiMock.getApiUpdateTest1());
        assertEquals(2, apiMock.getApiUpdateTest2());
    }

}