package org.walrex.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMerchantQrRequest {
    private String name;                   // Alias for the user (e.g., "Mi Plin")
    private Map<String, String> rawTags;  // All tags extracted during decode
}
