package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateDistritoRequest(
        @NotNull(message = "Es obligatorio el atributo idProvince")
        @NotBlank(message = "El atributo idProvince no puede ser 0")
        Integer idProvince,

        @NotNull(message = "Es obligatorio el atributo codigo de distrito")
        @NotEmpty(message = "Codigo no puede estar vacio")
        @Length(min = 6, max = 6, message = "Codigo debe contener 6 caracteres")
        @Pattern(regexp = "^[0-9]{6}$", message = "Codigo solo debe contener carácteres numéricos")
        String code,

        @NotNull(message = "Es obligatorio el atributo nombre del distrito")
        @NotEmpty(message = "El atributo nombre no puede estar vacio")
        @Length(min = 3, max = 100, message = "El atributo nombre debe contener entre 3 y 100 caracteres")
        String nombre

) {
}
