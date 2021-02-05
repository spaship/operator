package io.websitecd.operator.rest;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import io.restassured.http.ContentType;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(WebHookResource.class)
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookGitUrlUnknownTest extends GitlabWebhookTestCommon {

    @MockServer
    KubernetesMockServer mockServer;

    @Test
    public void ignoredGitUrl() throws Exception {
        OperatorServiceTest.setupMockServer(mockServer);
        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET)
                .body(GitlabWebHookGitUrlUnknownTest.class.getResourceAsStream("/gitlab-push-giturl-unknown.json"))
                .when().post()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("IGNORED"));
    }

}