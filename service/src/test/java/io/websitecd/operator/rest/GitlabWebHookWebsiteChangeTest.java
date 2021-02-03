package io.websitecd.operator.rest;

import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.quarkus.test.kubernetes.client.MockServer;
import io.vertx.core.Vertx;
import io.websitecd.operator.ContentApiMock;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.openshift.OperatorService;
import io.websitecd.operator.openshift.OperatorServiceTest;
import io.websitecd.operator.openshift.WebsiteConfigService;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(WebHookResource.class)
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookWebsiteChangeTest {

    @MockServer
    KubernetesMockServer mockServer;

    @Inject
    OperatorService operatorService;

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    ContentController contentController;

    @Test
    public void gitPushWebsiteChange() throws Exception {
        ContentApiMock apiMock = new ContentApiMock(contentController.getStaticContentApiPort());

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(apiMock);

        OperatorServiceTest.setupServerAdvanced(mockServer);

        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-dev")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();
        mockServer.expect()
                .patch().withPath("/apis/apps/v1/namespaces/websitecd-examples/deployments/simple-content-prod")
                .andReturn(200, new DeploymentSpecBuilder().build()).always();

        websiteConfigService.setConfigDir(Optional.of(OperatorServiceTest.GIT_EXAMPLES_CONFIG_SIMPLE));
        operatorService.initServices(OperatorServiceTest.GIT_EXAMPLES_URL, OperatorServiceTest.GIT_EXAMPLES_BRANCH);

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .body(GitlabWebHookWebsiteChangeTest.class.getResourceAsStream("/gitlab-push-website-changed.json"))
                .when().post()
                .then()
                .statusCode(200)
                .body(is("DONE"));

        assertEquals(0, apiMock.getApiListCount());
        assertEquals(0, apiMock.getApiUpdateTest1());
        assertEquals(0, apiMock.getApiUpdateTest2());

    }

}