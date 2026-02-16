package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateBeneficiaryAccountRequest(
        @NotNull
        Integer payoutRailId,

        Long bankId,

        @Size(max = 40)
        String accountNumber,

        @Size(max = 20)
        String phoneNumber,

        @NotNull
        Integer currencyId,

        Boolean isFavorite
) {
}
