package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateDepartamentRequest(
        @NotNull(message = "Es obligatorio el atributo codigo del departamento")
        @NotEmpty(message = "Codigo no puede estar vacio")
        @Length(min = 2, max = 2, message = "Codigo debe contener 2 caracteres")
        @Pattern(regexp = "^[0-9]{2}$", message = "Codigo solo debe contener carácteres numéricos")
        String code,

        @NotNull(message = "Es obligatorio el atributo nombre del departamento")
        @NotEmpty(message = "El atributo nombre no puede estar vacio")
        @Length(min = 3, max = 100, message = "El atributo nombre debe contener entre 3 y 100 caracteres")
        String nombre
) {
}
