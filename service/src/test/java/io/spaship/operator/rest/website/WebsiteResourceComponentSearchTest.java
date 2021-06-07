package io.spaship.operator.rest.website;

import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.ldap.MockInitialDirContextFactory;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Test of {@link WebsiteResource} - part /application
 */
@QuarkusTest
class WebsiteResourceComponentSearchTest extends WebhookTestCommon {

    protected String getApiUrl() {
        return WebsiteResource.getApplicationApiPath(EXAMPLES_NAMESPACE, EXAMPLES_SIMPLE, "dev");
    }

    @Test
    void notAuthenticated() {
        given()
                .when().get(getApiUrl())
                .then().log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void notAuthorized() {
        given().auth().oauth2(getAccessToken(AUTH_SPASHIP_USER, "invalid-role"))
                .when().get(getApiUrl())
                .then().log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void authorizedByLdap() throws Exception {
        registerSimpleWeb();

        given().auth().oauth2(getAccessToken(MockInitialDirContextFactory.LDAP_USER_ONLY))
                .when().get(getApiUrl())
                .then().log().ifValidationFails()
                .statusCode(200);
    }

    @Test
    void searchComponentAll() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb(false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(getApiUrl())
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(2));
    }

    @Test
    void searchComponentEmptyByFilterEnv() throws Exception {
        registerSimpleWeb();
        registerWeb(ADVANCED_WEBSITE, false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(WebsiteResource.getApplicationApiPath(EXAMPLES_NAMESPACE, EXAMPLES_SIMPLE, "some-bad-env"))
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
                .when().get(WebsiteResource.getApplicationApiPath(EXAMPLES_NAMESPACE, "some-bad-website", "dev"))
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }
}