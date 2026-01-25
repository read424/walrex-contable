package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcupacionUpdateRequest {

    @NotNull(message = "El ID de la ocupación no puede ser nulo.")
    private Long id;

    @NotBlank(message = "El código de la ocupación no puede estar vacío.")
    @Size(max = 5, message = "El código de la ocupación no puede exceder los 5 caracteres.")
    private String codigo;

    @NotBlank(message = "El nombre de la ocupación no puede estar vacío.")
    @Size(max = 100, message = "El nombre de la ocupación no puede exceder los 100 caracteres.")
    private String nombre;

    @NotNull(message = "El estado de la ocupación no puede ser nulo.")
    private Integer status;

}
