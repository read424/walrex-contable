package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateCalculationResponse {
    private CalculationResult calculation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationResult {
        private BigDecimal inputAmount;
        private String inputCurrency;
        private BigDecimal outputAmount;
        private String outputCurrency;
        private RateUsed rateUsed;
        private OffsetDateTime calculatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateUsed {
        private String code;
        private String name;
        private BigDecimal value;
        private String date;
    }
}