package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryAccount {
    private Long id;
    private Beneficiary beneficiary;
    private Integer payoutRailId;
    private Long bankId;
    private String accountNumber;
    private String phoneNumber;
    private Integer currencyId;
    private Boolean isFavorite;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
