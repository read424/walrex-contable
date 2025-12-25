package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Modelo de dominio para cuentas contables.
 *
 * Representa una cuenta contable en el plan de cuentas de la organización.
 * Cada cuenta tiene un código único, nombre descriptivo, tipo contable y lado normal.
 *
 * Soporta soft delete mediante el campo deletedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingAccount {

    /**
     * Identificador único de la cuenta
     */
    private Integer id;

    /**
     * Código único de la cuenta (ej: 1010, 2020, etc.)
     * Máximo 20 caracteres, formato libre
     */
    private String code;

    /**
     * Nombre descriptivo de la cuenta
     * Máximo 200 caracteres
     */
    private String name;

    /**
     * Tipo contable de la cuenta
     * Define la naturaleza económica: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
     */
    private AccountType type;

    /**
     * Lado normal de la cuenta
     * Indica si los aumentos se registran al debe (DEBIT) o al haber (CREDIT)
     */
    private NormalSide normalSide;

    /**
     * Indica si la cuenta está activa
     * Las cuentas inactivas no se pueden usar en nuevos asientos contables
     */
    private Boolean active;

    /**
     * Fecha de creación del registro
     */
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    private OffsetDateTime updatedAt;

    /**
     * Fecha de eliminación lógica del registro
     * Si es null, el registro no ha sido eliminado
     */
    private OffsetDateTime deletedAt;

    /**
     * Verifica si la cuenta está eliminada (soft delete)
     *
     * @return true si la cuenta ha sido eliminada lógicamente
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Verifica si la cuenta está activa y no eliminada
     *
     * @return true si la cuenta está activa y no ha sido eliminada
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }
}
