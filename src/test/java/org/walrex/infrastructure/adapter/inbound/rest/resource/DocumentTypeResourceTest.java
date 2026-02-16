package org.walrex.infrastructure.adapter.inbound.rest.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class DocumentTypeResourceTest {

    @Test
    void testGetDocumentTypesEndpoint() {
        given()
                .queryParam("countryIso2", "VE")
                .when()
                .get("/api/v1/document-types")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(notNullValue());
    }

    @Test
    void testGetDocumentTypesMissingParam() {
        given()
                .when()
                .get("/api/v1/document-types")
                .then()
                .statusCode(400);
    }
}
