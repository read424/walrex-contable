package org.walrex.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateRateRequest {
    private String countryIso2;
    private String rateCode;
    private BigDecimal amount;
    private String baseCurrency;
}