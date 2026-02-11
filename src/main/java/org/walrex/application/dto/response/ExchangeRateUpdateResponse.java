package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateUpdateResponse {
    private String originCurrency;
    private String destinationCurrency;
    private String rateType;
    private BigDecimal rate;
    private Long timestamp;
}