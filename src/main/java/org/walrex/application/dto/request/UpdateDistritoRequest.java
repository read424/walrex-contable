package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record UpdateDistritoRequest(
        @NotNull(message = "Es obligatorio indicar id de la provincia")
        Integer idProvincia,

        @NotNull(message = "Es obligatorio indicar el codigo ubigeo del distrito")
        @NotEmpty(message = "El codigo distrito no puede ser vacio")
        @Length(min = 6, max = 6, message = "El codigo debe contener 6 caracteres")
        @Pattern(regexp = "^[0-9]{6}$" , message = "El codigo solo debe contener caracteres numerico")
        String codigo,

        @NotNull(message = "Es obligatorio indicar el nombre de la distrito")
        @NotEmpty(message = "El atributo nombre distrito no puede ser vacio")
        @Length(min = 5, max = 100, message = "Nombre distrito debe contener entre 5 y 100 caracteres")
        String nombre
) {
}
