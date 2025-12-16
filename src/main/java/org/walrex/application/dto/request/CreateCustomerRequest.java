package org.walrex.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        Long id,

        @NotNull(message = "El tipo de documento es obligatorio")
        Integer idTypeDocument,

        @NotNull(message = "El número de documento es obligatorio")
        @Size(min = 8, max = 12, message = "El número de documento debe tener entre 8 y 15 caracteres")
        @Pattern(regexp = "^[0-9]+$", message = "El número de documento debe contener solo números")
        String numberDocument,

        @NotNull(message = "El nombre/razon social es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre/razon social debe tener entre 2 y 100 caracteres")
        String firstName,

        @NotNull(message = "El apellido/nombre comercial es obligatorio")
        @Size(min = 2, max = 100, message = "El apellido/nombre comercial debe tener entre 2 y 100 caracteres")
        String lastName,

        String address,

        @NotNull(message = "El departamento es obligatorio")
        Integer idCountryDepartment,

        @NotNull(message = "La provincia es obligatoria")
        Integer idCountryProvince,

        @NotNull(message = "El distrito es obligatorio")
        Integer idCountryDistrict,

        @NotNull(message = "El correo es obligatorio")
        @Email(message = "El correo debe ser válido")
        String email,

        @NotNull(message = "El número de teléfono es obligatorio")
        @Pattern(regexp = "^[0-9]{8,12}$", message = "El número de teléfono debe contener exactamente 8 a 12 dígitos")
        String phoneNumber,

        @NotNull(message = "El número de teléfono móvil es obligatorio")
        @Pattern(regexp = "^[0-9]{8,12}$", message = "El número de teléfono debe contener exactamente 8 a 12 dígitos")
        String phoneMobile
) {
}
