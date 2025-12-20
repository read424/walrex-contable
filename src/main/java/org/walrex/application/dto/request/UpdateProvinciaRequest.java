package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record UpdateProvinciaRequest(
        @NotNull(message = "Es obligatorio indicar id del departamento")
        Integer idDepartamento,

        @NotNull(message = "Es obligatorio indicar el codigo ubigeo de la provincia")
        @NotEmpty(message = "El codigo provincia no puede ser vacio")
        @Length(min = 6, max = 6, message = "El codigo debe contener 6 caracteres")
        String codigo,

        @NotNull(message = "Es obligatorio indicar el nombre de la provincia")
        @NotEmpty(message = "El atributo nombre provincia no puede ser vacio")
        @Length(min = 5, max = 100, message = "Nombre provincia debe contener entre 5 y 100 caracteres")
        String nombre
) {
}
