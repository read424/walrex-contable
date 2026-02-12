package org.walrex.infrastructure.adapter.inbound.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.walrex.application.dto.request.GenerateQrRequest;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class EmvQrResourceTest {

    @Test
    void testGenerateEndpoint() {
        GenerateQrRequest request = GenerateQrRequest.builder()
                .merchantId("123456")
                .merchantName("WALREX TEST")
                .city("LIMA")
                .qrType(GenerateQrRequest.QrType.STATIC)
                .amount(new BigDecimal("10.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/qr-emv/generate")
                .then()
                .statusCode(200)
                .body(containsString("000201"))
                .body(containsString("540510.00"));
    }

    @Test
    void testDecodeEndpoint() {
        String qrCode = "0002010102113906123456520454115303604540510.005802PE5911WALREX TEST6004LIMA6304BFAD";
        org.walrex.application.dto.request.DecodeQrRequest request = 
            org.walrex.application.dto.request.DecodeQrRequest.builder()
                .qrCodeText(qrCode)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/qr-emv/decode")
                .then()
                .statusCode(200)
                .body("merchantName", is("WALREX TEST"))
                .body("amount", is(10.0f))
                .body("valid", is(true));
    }
}
