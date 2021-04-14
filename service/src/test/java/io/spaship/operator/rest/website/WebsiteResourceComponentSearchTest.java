package io.spaship.operator.rest.website;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.ldap.MockInitialDirContextFactory;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Test of {@link WebsiteResource} - part component/search
 */
@QuarkusTest
@TestHTTPEndpoint(WebsiteResource.class)
class WebsiteResourceComponentSearchTest extends WebhookTestCommon {

    String COMPONENT_API_SEARCH = "/component/search";

    @Test
    void notAuthenticated() {
        given()
                .when().get(COMPONENT_API_SEARCH)
                .then().log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void notAuthorized() {
        given().auth().oauth2(getAccessToken(AUTH_SPASHIP_USER, "invalid-role"))
                .when().get(COMPONENT_API_SEARCH)
                .then().log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void authorizedByLdap() throws Exception {
        registerSimpleWeb();

        given().auth().oauth2(getAccessToken(MockInitialDirContextFactory.LDAP_USERNAME))
                .when().get(COMPONENT_API_SEARCH + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=dev")
                .then().log().ifValidationFails()
                .statusCode(200);
    }

    @Test
    void badInput() {
        websiteRepository.reset();

        given().auth().oauth2(getSpashipUserToken())
                .when().get(COMPONENT_API_SEARCH)
                .then().log().ifValidationFails()
                .statusCode(400)
                .body(is("input parameters namespace, website, env are required"));
    }

    @Test
    void searchComponentAll() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb(false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(COMPONENT_API_SEARCH + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=dev")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(2));
    }

    @Test
    void searchComponentByName() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb(false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(COMPONENT_API_SEARCH + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=dev&name=theme")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(1))
                .body("data[0].name", is("theme"))
                .body("data[0].path", is("/theme"))
                .body("data[0].ref", is("main"))
                .body("data[0].api", is("http://test.info/api/v1/website/component/info?namespace=spaship-examples&website=simple&env=dev&name=theme"));
    }

    @Test
    void searchComponentEmptyByFilterName() throws Exception {
        registerSimpleWeb();
        registerWeb(ADVANCED_WEBSITE, false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(COMPONENT_API_SEARCH + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=dev&name=some-wrong-name")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }

    @Test
    void searchComponentEmptyByFilterEnv() throws Exception {
        registerSimpleWeb();
        registerWeb(ADVANCED_WEBSITE, false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(COMPONENT_API_SEARCH + "?namespace=" + EXAMPLES_NAMESPACE + "&website=simple&env=some-bad-env")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }

    @Test
    void searchComponentEmptyByFilterWebsite() throws Exception {
        registerSimpleWeb();
        registerWeb(ADVANCED_WEBSITE, false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(COMPONENT_API_SEARCH + "?namespace=" + EXAMPLES_NAMESPACE + "&website=some-bad-website&env=dev")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }
}