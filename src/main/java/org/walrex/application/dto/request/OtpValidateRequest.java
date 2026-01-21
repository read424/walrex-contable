package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.walrex.domain.model.OtpPurpose;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OtpValidateRequest {

    @NotBlank
    private String referenceId;

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;

    @NotNull
    private OtpPurpose purpose;
}