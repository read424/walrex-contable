package org.walrex.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCurrencyRequest {

    private Integer currencyId;
    private Boolean isPrimary;
    private Boolean isOperational;
    private LocalDate effectiveDate;
}
