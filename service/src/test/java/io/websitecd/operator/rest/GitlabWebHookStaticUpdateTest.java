package io.websitecd.operator.rest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesMockServerTestResource;
import io.restassured.http.ContentType;
import io.websitecd.operator.content.ContentController;
import io.websitecd.operator.openshift.OperatorServiceTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class GitlabWebHookStaticUpdateTest extends GitlabWebhookTestCommon {

    @Inject
    ContentController contentController;

    @Test
    public void gitPushStaticUpdate() throws Exception {
        registerSimpleWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_SIMPLE)
                .body(GitlabWebHookStaticUpdateTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("components.size()", is(2));

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateThemeCount());
        assertEquals(1, apiMock.getApiUpdateRootCount());
    }

    @Test
    public void gitPushStaticUpdateAdvanced() throws Exception {
        registerAdvancedWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_ADVANCED)
                .body(GitlabWebHookStaticUpdateTest.class.getResourceAsStream("/gitlab-push.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("components.size()", is(3));  // template, search, _root

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateThemeCount());
        assertEquals(1, apiMock.getApiUpdateSearchCount());
        assertEquals(1, apiMock.getApiUpdateRootCount());
        assertEquals(0, apiMock.getApiUpdateSharedCount());
    }

    @Test
    public void gitPushStaticUpdateAdvancedTag() throws Exception {
        registerAdvancedWeb();

        given()
                .header("Content-type", "application/json")
                .header("X-Gitlab-Event", "Push Hook")
                .header("X-Gitlab-Token", OperatorServiceTest.SECRET_ADVANCED)
                .body(GitlabWebHookStaticUpdateTest.class.getResourceAsStream("/gitlab-push-tag.json"))
                .when().post("/api/webhook")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("SUCCESS"))
                .body("components.size()", is(1))
                .body("components[0].name", is("shared-components"));

        assertEquals(0, apiMock.getApiListCount());
        // only dev has same branch
        assertEquals(1, apiMock.getApiUpdateSharedCount());
        assertEquals(0, apiMock.getApiUpdateThemeCount());
        assertEquals(0, apiMock.getApiUpdateSearchCount());
        assertEquals(0, apiMock.getApiUpdateRootCount());
    }

}