package io.websitecd.operator.rest;

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
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(WebHookResource.class)
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookResourceTest {

    private static final Logger log = Logger.getLogger(GitlabWebHookResourceTest.class);


    @MockServer
    KubernetesMockServer mockServer;

    @Inject
    OperatorService operatorService;

    @Inject
    WebsiteConfigService websiteConfigService;

    @Inject
    ContentController contentController;

    @Test
    public void gitPush() throws Exception {
        ContentApiMock apiMock = new ContentApiMock(contentController.getStaticContentApiPort());


        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(apiMock);

        OperatorServiceTest.setupServerAdvanced(mockServer);
        websiteConfigService.setConfigDir(Optional.of(OperatorServiceTest.GIT_EXAMPLES_CONFIG_SIMPLE));
        operatorService.initServices(OperatorServiceTest.GIT_EXAMPLES_URL, OperatorServiceTest.GIT_EXAMPLES_BRANCH);

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .body(GitlabWebHookResourceTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post()
                .then()
                .statusCode(200)
                .body(is("DONE"));

        Assert.assertEquals(2, apiMock.getApiListCount());
        Assert.assertEquals(2, apiMock.getApiUpdateTest1());
        Assert.assertEquals(2, apiMock.getApiUpdateTest2());
    }

}