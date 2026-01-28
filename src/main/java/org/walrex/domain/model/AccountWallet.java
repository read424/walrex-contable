package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class AccountWallet {

    private Long id;

    private Integer clientId;

    private Integer countryId;

    private Integer currencyId;

    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Builder.Default
    private Boolean isBalanceVisible = true;

    private String bankAccountNumber;

    @Builder.Default
    private String status = "ACTIVE";

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
