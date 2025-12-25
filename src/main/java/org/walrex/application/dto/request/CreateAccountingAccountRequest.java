package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.walrex.domain.model.AccountType;
import org.walrex.domain.model.NormalSide;

/**
 * DTO para crear una nueva cuenta contable.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad (código y nombre) se hace en el Service.
 */
public record CreateAccountingAccountRequest(
    /**
     * Código único de la cuenta.
     * - Máximo 20 caracteres
     * - Formato libre (sin restricciones de patrón)
     * - Ej: "1010", "2020", "CAJA-01", etc.
     */
    @NotBlank(message = "Code is required")
    @Size(min = 1, max = 20, message = "Code must be between 1 and 20 characters")
    String code,

    /**
     * Nombre descriptivo de la cuenta.
     * - Entre 2 y 200 caracteres
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    String name,

    /**
     * Tipo contable de la cuenta.
     * - Obligatorio
     * - Valores: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
     */
    @NotNull(message = "Type is required")
    AccountType type,

    /**
     * Lado normal de la cuenta.
     * - Obligatorio
     * - Valores: DEBIT, CREDIT
     */
    @NotNull(message = "Normal side is required")
    NormalSide normalSide,

    /**
     * Indica si la cuenta está activa.
     * - Opcional (por defecto será true si no se especifica)
     */
    Boolean active

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public CreateAccountingAccountRequest {
        if (code != null) {
            code = code.trim();
        }
        if (name != null) {
            name = name.trim();
        }
        // Si active es null, establecer true como valor por defecto
        if (active == null) {
            active = true;
        }
    }
}
