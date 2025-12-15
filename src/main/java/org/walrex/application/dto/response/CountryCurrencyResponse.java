package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryCurrencyResponse {

    private Long id;
    private Integer countryId;
    private Integer currencyId;
    private Boolean isPrimary;
    private Boolean isOperational;
    private LocalDate effectiveDate;
    private OffsetDateTime createdAt;

    // Datos denormalizados de la moneda
    private String currencyCode;
    private String currencyName;
}
