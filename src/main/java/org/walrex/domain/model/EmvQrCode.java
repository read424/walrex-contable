package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class EmvQrCode {
    private String payloadFormatIndicator;            // ID 00
    private String pointOfInitiationMethod;           // ID 01
    private Map<String, String> merchantAccountInfo;  // IDs 02-51
    private String merchantCategoryCode;              // ID 52
    private String transactionCurrency;               // ID 53 (ISO 4217 numeric)
    private BigDecimal transactionAmount;             // ID 54
    private String tipOrConvenienceIndicator;         // ID 55
    private BigDecimal valueOfConvenienceFeeFixed;   // ID 56
    private BigDecimal valueOfConvenienceFeePercentage; // ID 57
    private String countryCode;                       // ID 58 (ISO 3166-1 alpha-2)
    private String merchantName;                      // ID 59
    private String merchantCity;                      // ID 60
    private String postalCode;                        // ID 61
    private String additionalDataFieldTemplate;       // ID 62
    private Map<String, String> rawTags;              // All raw tags for debugging/Plin
}
