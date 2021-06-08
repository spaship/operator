package io.spaship.operator.rest.website;

import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.ldap.MockInitialDirContextFactory;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Test of {@link WebsiteResource} - part /application/:name
 */
@QuarkusTest
class WebsiteResourceComponentInfoTest extends WebhookTestCommon {

    protected String getApiUrl() {
        return WebsiteResource.getApplicationDetailApiPath(EXAMPLES_NAMESPACE, EXAMPLES_SIMPLE, "dev", "theme");
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

        given().auth().oauth2(getSpashipUserToken())
                .when().get(WebsiteResource.getApplicationDetailApiPath(EXAMPLES_NAMESPACE, EXAMPLES_SIMPLE, "dev", "some-wrong-name"))
                .then().log().ifValidationFails()
                .statusCode(404);
    }

}
