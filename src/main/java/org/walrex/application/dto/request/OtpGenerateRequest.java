package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.walrex.domain.model.OtpChannel;
import org.walrex.domain.model.OtpPurpose;
import org.walrex.domain.model.OtpTargetType;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OtpGenerateRequest {

    @NotNull
    private OtpTargetType targetType;

    @NotBlank
    private String target;

    @NotNull
    private OtpChannel channel;

    @NotNull
    private OtpPurpose purpose;
}