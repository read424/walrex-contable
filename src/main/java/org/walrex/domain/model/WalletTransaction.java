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
public class WalletTransaction {

    private Long id;

    private Long referenceId;

    private Long walletId;

    private String counterpartyReference;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String operationType;

    private String operationDirection;

    @Builder.Default
    private String status = "COMPLETED";

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
