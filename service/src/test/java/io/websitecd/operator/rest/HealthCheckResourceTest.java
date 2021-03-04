package io.websitecd.operator.rest;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.websitecd.operator.QuarkusTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(HealthCheckResource.class)
class HealthCheckResourceTest extends QuarkusTestBase {

    @Test
    void live() {
        given()
                .when().get("/live")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(is("live"));
    }

    @Test
    void ready() {
        given()
                .when().get("/ready")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(is("ready"));
    }
}