package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Domain model for country_currency to payment method relationship
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CountryCurrencyPaymentMethod {
    private Long id;
    private Long countryCurrencyId;
    private Long bankId;
    private String isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Denormalized for convenience
    private String bankPaymentCode;  // bank.name_pay_binance
    private String bankName;         // bank.det_name
}
