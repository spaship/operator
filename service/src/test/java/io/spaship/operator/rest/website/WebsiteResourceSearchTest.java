package io.spaship.operator.rest.website;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.spaship.operator.crd.Website;
import io.spaship.operator.rest.WebhookTestCommon;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Test of {@link WebsiteResource} - part search
 */
@QuarkusTest
@TestHTTPEndpoint(WebsiteResource.class)
class WebsiteResourceSearchTest extends WebhookTestCommon {

    String SEARCH_API = "/search";

    @Test
    void searchEmpty() {
        websiteRepository.reset();
        given()
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
        given()
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
        given()
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
        given()
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
        given()
                .when().get(SEARCH_API + "?name=some-wrong-name")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("status", is("success"))
                .body("data.size()", is(0));
    }

}