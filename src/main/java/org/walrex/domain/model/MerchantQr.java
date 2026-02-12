package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class MerchantQr {
    private Long id;
    private String name;              // Alias given by the user (e.g., "My Plin")
    private String merchantName;      // Tag 59
    private String merchantCity;      // Tag 60
    private String mcc;               // Tag 52
    private String currency;          // Tag 53
    private String countryCode;       // Tag 58
    private String payloadFormatIndicator; // Tag 00
    private String pointOfInitiationMethod; // Tag 01
    private Map<String, String> accountInfo; // Tags 02-51 (Store all sub-tags here)
}
