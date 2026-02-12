package org.walrex.domain.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExchangeRateRouteInfo extends RemittanceRoute {
    private Integer countryToId;
    private String countryToCode;
}
