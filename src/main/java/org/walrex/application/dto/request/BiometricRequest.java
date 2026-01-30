package org.walrex.application.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricRequest {

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    private String biometricType;

    @AssertTrue(message = "biometricType is required when enabled is true")
    private boolean isBiometricTypeValid() {
        return !Boolean.TRUE.equals(enabled) || (biometricType != null && !biometricType.isBlank());
    }
}
