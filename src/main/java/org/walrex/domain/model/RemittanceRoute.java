package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemittanceRoute {
    private Long countryCurrencyFromId;
    private Integer currencyFromId;
    private String currencyFromCode;
    private Long countryCurrencyToId;
    private Integer currencyToId;
    private String currencyToCode;
    private String intermediaryAsset;
}
