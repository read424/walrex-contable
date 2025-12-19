package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record UpdateDepartamentRequest(
        @NotNull(message = "Es obligatorio indicar el codigo ubigeo del departamento")
        @NotEmpty(message = "Codigo departamento no puede ser vacio")
        @Length(min = 6, max = 6, message = "El codigo debe contener 6 caracteres")
        String codigo,

        @NotNull(message = "Es obligatorio indicar el nombre del departamento")
        @NotEmpty(message = "Nombre departamento no puede ser vacio")
        @Length(min = 5, max = 100, message = "Nombre departamento debe contener entre 5 y 100 caracteres")
        String nombre
) {
}
