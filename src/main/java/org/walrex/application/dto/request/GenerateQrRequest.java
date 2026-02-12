package org.walrex.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQrRequest {
    private String merchantId;
    private String merchantName;
    private String city;
    private QrType qrType;
    private BigDecimal amount;
    private String details;
    private String payloadFormatIndicator; // Default: 01
    private String mcc;                    // Default: 5611
    private String currency;               // Default: 604
    private String countryCode;            // Default: PE

    public enum QrType {
        STATIC, DYNAMIC
    }
}
