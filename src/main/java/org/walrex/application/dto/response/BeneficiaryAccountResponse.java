package org.walrex.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BeneficiaryAccountResponse(
        Integer id,
        Long customerId,
        String customerName,
        Long bankId,
        String bankName,
        Integer typeAccountId,
        String typeAccountName,
        String accountNumber,
        String beneficiaryLastName,
        String beneficiarySurname,
        String idNumber,
        String status,
        Integer typeOperationId,
        String typeOperationName,
        String isAccountMe
) {
}
