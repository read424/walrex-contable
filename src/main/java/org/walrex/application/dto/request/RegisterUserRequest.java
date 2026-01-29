package org.walrex.application.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.walrex.domain.model.IdentificationMethod;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class RegisterUserRequest {

    @NotNull(message = "El método de identificación es requerido")
    private IdentificationMethod identificationMethod;

    @NotBlank(message = "El referenceId es requerido")
    private String referenceId;

    @NotBlank(message = "El token de registro es requerido")
    private String registrationToken;

    @NotNull(message = "La fecha de expiración del token es requerida")
    private OffsetDateTime tokenExpiresAt;

    @NotBlank(message = "El nombre es requerido")
    private String names;

    private String firstLastName;

    private String secondLastName;

    @NotBlank(message = "La fecha de nacimiento es requerida")
    private String birthDate;

    @NotBlank(message = "La ocupación es requerida")
    private String occupation;

    @NotNull(message = "Debe indicar si es PEP")
    private Boolean isPoliticallyExposed;

    @AssertTrue(message = "Debe aceptar los términos y condiciones")
    private Boolean acceptedTermsAndConditions;

    @AssertTrue(message = "Debe aceptar la política de privacidad")
    private Boolean acceptedPrivacyPolicy;

    @NotBlank(message = "El PIN es requerido")
    private String pinHash;

    @NotNull(message = "El país es requerido")
    private Integer countryId;
}
