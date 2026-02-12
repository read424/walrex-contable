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
public class DecodeQrResponse {
    private String merchantId;
    private String merchantName;
    private String city;
    private BigDecimal amount;
    private String currency;
    private boolean valid;
    private java.util.Map<String, String> rawTags; // For debugging/Plin tags
}
