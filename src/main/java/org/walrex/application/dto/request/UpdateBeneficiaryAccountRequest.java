package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateBeneficiaryAccountRequest(
        @NotNull
        Long customerId,

        Long bankId,

        Integer typeAccountId,

        @NotBlank
        @Size(max = 25)
        String accountNumber,

        @Size(max = 60)
        String beneficiaryLastName,

        @Size(max = 50)
        String beneficiarySurname,

        @NotBlank
        @Size(max = 15)
        String idNumber,

        @NotNull
        Integer typeOperationId,

        @NotBlank
        @Size(min = 1, max = 1)
        String isAccountMe,
        
        @NotBlank
        @Size(min = 1, max = 1)
        String status
) {
}
