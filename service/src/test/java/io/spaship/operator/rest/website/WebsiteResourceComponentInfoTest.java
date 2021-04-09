package io.spaship.operator.rest.website;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Test of {@link WebsiteResource} - part component/info
 */
@QuarkusTest
@TestHTTPEndpoint(WebsiteResource.class)
class WebsiteResourceComponentInfoTest extends WebhookTestCommon {

    String COMPONENT_API_INFO = "/component/info";

    @Test
    void badInput() {
        websiteRepository.reset();
        given()
                .when().get(COMPONENT_API_INFO)
                .then().log().ifValidationFails()
                .statusCode(400)
                .body(is("input parameters namespace, website, env, name are required"));
    }

    @Test
    void searchComponentAll() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb(false);
        given()
                .when().get(COMPONENT_API_INFO + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=dev&name=theme")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.name", is("theme"))
//                .body("data.path", is("/theme"))
                .body("data.ref", is("main"))
                .body("data.gitUrl", is("giturl"))
                .body("data.timestamp", is(not(empty())))
                .body("data.lastCommitMessage", is("gitMessage"))
                .body("data.lastCommitAuthor", is("gitAuthor"));
    }

    @Test
    void searchComponentEmptyByFilter() throws Exception {
        registerSimpleWeb();
        registerWeb(ADVANCED_WEBSITE, false);
        given()
                .when().get(COMPONENT_API_INFO + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=dev&name=some-wrong-name")
                .then().log().ifValidationFails()
                .statusCode(404);
    }

}
