package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.walrex.domain.model.AccountType;
import org.walrex.domain.model.NormalSide;

/**
 * DTO para actualizar una cuenta contable existente.
 *
 * Usa las mismas validaciones que CreateAccountingAccountRequest.
 * En un PUT, todos los campos son obligatorios (reemplazo completo).
 * Para PATCH (actualización parcial) usaríamos campos opcionales.
 */
public record UpdateAccountingAccountRequest(

        @NotBlank(message = "Code is required")
        @Size(min = 1, max = 20, message = "Code must be between 1 and 20 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
        String name,

        @NotNull(message = "Type is required")
        AccountType type,

        @NotNull(message = "Normal side is required")
        NormalSide normalSide,

        /**
         * Permite activar/desactivar la cuenta sin eliminarla.
         */
        Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public UpdateAccountingAccountRequest {
        if (code != null) {
            code = code.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        if (active == null) {
            active = true;
        }
    }
}
