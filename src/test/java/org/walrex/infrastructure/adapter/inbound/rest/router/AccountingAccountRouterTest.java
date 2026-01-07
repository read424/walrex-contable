package org.walrex.infrastructure.adapter.inbound.rest.router;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.walrex.application.port.input.ListAccountingAccountsUseCase;
import org.walrex.application.dto.query.AccountingAccountFilter;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class AccountingAccountRouterTest {

    @InjectMock
    ListAccountingAccountsUseCase listAccountsUseCase;

    @Test
    public void testFindAll_whenUseCaseFails_returnsInternalServerError() {
        // Mock the use case to return a failure
        Mockito.when(listAccountsUseCase.findAll(Mockito.any(AccountingAccountFilter.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Database connection failed")));

        // Perform the GET request and verify the response
        RestAssured.given()
                .when().get("/api/v1/accountingAccounts/all")
                .then()
                .statusCode(500)
                .body("error", is("Internal Server Error"))
                .body("message", is("Database connection failed"));
    }

    @Test
    public void testList_whenUseCaseFails_returnsInternalServerError() {
        // Mock the use case to return a failure
        Mockito.when(listAccountsUseCase.execute(Mockito.any(), Mockito.any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Paging logic failed")));

        // Perform the GET request and verify the response
        RestAssured.given()
                .queryParam("page", 1)
                .queryParam("size", 10)
                .when().get("/api/v1/accountingAccounts")
                .then()
                .statusCode(500)
                .body("error", is("Internal Server Error"))
                .body("message", is("Paging logic failed"));
    }
}
