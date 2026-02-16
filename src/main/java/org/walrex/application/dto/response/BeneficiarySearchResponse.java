package org.walrex.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BeneficiarySearchResponse(
        Long beneficiaryId,
        Long accountId,
        String fullName,
        String alias,
        String rail,
        String bank,
        String maskedAccount,
        Boolean isFavorite
) {
}
