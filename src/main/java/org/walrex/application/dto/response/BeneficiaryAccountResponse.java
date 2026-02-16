package org.walrex.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BeneficiaryAccountResponse(
        Long id,
        Long beneficiaryId,
        Integer payoutRailId,
        Long bankId,
        String accountNumber,
        String phoneNumber,
        Integer currencyId,
        Boolean isFavorite,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
