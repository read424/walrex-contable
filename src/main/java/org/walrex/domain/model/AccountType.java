package org.walrex.domain.model;

/**
 * Tipos de cuenta contable según principios contables estándar.
 *
 * Representa la naturaleza económica de una cuenta en el sistema contable:
 * - ASSET: Activos (recursos económicos controlados por la entidad)
 * - LIABILITY: Pasivos (obligaciones actuales de la entidad)
 * - EQUITY: Patrimonio (participación residual en los activos)
 * - REVENUE: Ingresos (aumentos en los beneficios económicos)
 * - EXPENSE: Gastos (disminuciones en los beneficios económicos)
 */
public enum AccountType {

    /**
     * Activo - Recursos económicos controlados por la entidad
     */
    ASSET("Activo"),

    /**
     * Pasivo - Obligaciones actuales de la entidad
     */
    LIABILITY("Pasivo"),

    /**
     * Patrimonio - Participación residual en los activos de la entidad
     */
    EQUITY("Patrimonio"),

    /**
     * Ingreso - Aumentos en los beneficios económicos
     */
    REVENUE("Ingreso"),

    /**
     * Gasto - Disminuciones en los beneficios económicos
     */
    EXPENSE("Gasto");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Obtiene el nombre para mostrar en la UI.
     *
     * @return Nombre en español para mostrar al usuario
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte un string a AccountType de forma segura.
     *
     * @param value Valor a convertir
     * @return AccountType correspondiente o null si no existe
     */
    public static AccountType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return AccountType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
