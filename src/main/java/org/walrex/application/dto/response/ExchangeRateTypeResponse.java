package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateTypeResponse {
    private Integer id;
    private String code;
    private String name;
    private BigDecimal rateValue;
    private String baseCurrency;
    private LocalDate dateRate;
    private Integer displayOrder;
}