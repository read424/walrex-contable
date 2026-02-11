package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateSource {
    private Long id;
    private Integer countryId;
    private Integer currencyId;
    private String sourceCode;
    private String sourceName;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    // Relaciones
    private Country country;
    private Currency currency;

    public static ExchangeRateSource create(Integer countryId, Integer currencyId, String sourceCode, 
                                          String sourceName, String description, Integer displayOrder) {
        return ExchangeRateSource.builder()
                .countryId(countryId)
                .currencyId(currencyId)
                .sourceCode(sourceCode)
                .sourceName(sourceName)
                .description(description)
                .displayOrder(displayOrder)
                .isActive(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }
}