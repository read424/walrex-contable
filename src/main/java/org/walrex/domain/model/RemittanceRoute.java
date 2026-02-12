package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RemittanceRoute {
    private Long countryCurrencyFromId;
    private Integer countryFromId;
    private String countryFromCode;
    private Integer currencyFromId;
    private String currencyFromCode;
    private Long countryCurrencyToId;
    private Integer currencyToId;
    private String currencyToCode;
    private String intermediaryAsset;
}
