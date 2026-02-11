package org.walrex.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class ExchangeRateType {
    private Integer id;
    private Integer countryId;
    private LocalDate dateRate;
    private String codeRate;
    private String nameRate;
    private BigDecimal rateValue;
    private Integer baseCurrencyId;
    private String status;
    private Integer displayOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Relaciones
    private Country country;
    private Currency baseCurrency;
}