package io.spaship.operator.rest.website;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.spaship.operator.crd.Website;
import io.spaship.operator.ldap.MockInitialDirContextFactory;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Test of {@link WebsiteResource} - part search
 */
@QuarkusTest
@TestHTTPEndpoint(WebsiteResource.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
class WebsiteResourceSearchTest extends WebhookTestCommon {

    String SEARCH_API = "/search";

    @Test
    void notAuthenticated() {
        given()
                .when().get(SEARCH_API)
                .then().log().ifValidationFails()
                .statusCode(401);
    }

    @Test
    void notAuthorized() {
        given().auth().oauth2(getAccessToken(AUTH_SPASHIP_USER, "invalid-role"))
                .when().get(SEARCH_API)
                .then().log().ifValidationFails()
                .statusCode(403);
    }

    @Test
    void authorizedByLdap() {
        given().auth().oauth2(getAccessToken(MockInitialDirContextFactory.LDAP_USERNAME))
                .when().get(SEARCH_API)
                .then().log().ifValidationFails()
                .statusCode(200);
    }

    @Test
    void searchEmpty() {
        websiteRepository.reset();
        given().auth().oauth2(getSpashipUserToken())
                .when().get(SEARCH_API)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }

    @Test
    void searchWebsiteAll() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb(false);
        given().auth().oauth2(getSpashipUserToken())
                .when().get(SEARCH_API)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(2));
    }

    @Test
    void searchWebsiteFilterName() throws Exception {
        registerSimpleWeb();
        registerAdvancedWeb(false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(SEARCH_API + "?name=simple")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(1))
                .body("data[0].spec.name", is("simple"))
                .body("data[0].spec.namespace", is(EXAMPLES_NAMESPACE));
    }

    @Test
    void searchWebsiteFilterNamespace() throws Exception {
        registerSimpleWeb();
        String namespace = "another-namespace";
        setupMockServerDeployment(namespace);
        Website advancedWebsite = ADVANCED_WEBSITE;
        advancedWebsite.getMetadata().setNamespace(namespace);
        registerWeb(advancedWebsite, false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(SEARCH_API + "?namespace=" + namespace)
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(1))
                .body("data[0].spec.name", is("advanced"))
                .body("data[0].spec.namespace", is(namespace));
    }

    @Test
    void searchWebsiteEmptyByFilter() throws Exception {
        registerSimpleWeb();
        registerWeb(ADVANCED_WEBSITE, false);

        given().auth().oauth2(getSpashipUserToken())
                .when().get(SEARCH_API + "?name=some-wrong-name")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }

}