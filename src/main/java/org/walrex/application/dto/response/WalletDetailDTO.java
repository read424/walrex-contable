package org.walrex.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDetailDTO {
    private Long walletId;
    private String countryCode;
    private String currencyCode;
    private BigDecimal balance;
    private Boolean viewBalance;
    private Boolean isDefault;
}
