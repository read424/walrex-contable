package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class WalletCountryConfig {

    private Integer id;

    private Integer countryId;

    private Integer currencyId;

    @Builder.Default
    private Boolean isDefault = false;

    @Builder.Default
    private Boolean enabled = true;
}
