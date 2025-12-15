package org.walrex.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class CountryCurrency {

    private Long id;
    private Integer countryId;
    private Integer currencyId;
    private Boolean isPrimary;
    private Boolean isOperational;
    private LocalDate effectiveDate;
    private OffsetDateTime createdAt;

    // Datos denormalizados para facilitar el response
    private String currencyCode;
    private String currencyName;
}