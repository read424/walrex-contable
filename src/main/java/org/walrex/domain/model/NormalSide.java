package org.walrex.domain.model;

/**
 * Lado normal de una cuenta contable.
 *
 * Indica si los aumentos en la cuenta se registran en el debe o en el haber:
 * - DEBIT: Debe (izquierda) - Aumentos en activos y gastos
 * - CREDIT: Haber (derecha) - Aumentos en pasivos, patrimonio e ingresos
 *
 * Reglas contables estándar:
 * - ASSET + EXPENSE → lado normal DEBIT
 * - LIABILITY + EQUITY + REVENUE → lado normal CREDIT
 */
public enum NormalSide {

    /**
     * Debe - Lado izquierdo de la ecuación contable.
     * Aumentos en activos y gastos se registran al debe.
     */
    DEBIT("Debe"),

    /**
     * Haber - Lado derecho de la ecuación contable.
     * Aumentos en pasivos, patrimonio e ingresos se registran al haber.
     */
    CREDIT("Haber");

    private final String displayName;

    NormalSide(String displayName) {
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
     * Convierte un string a NormalSide de forma segura.
     *
     * @param value Valor a convertir
     * @return NormalSide correspondiente o null si no existe
     */
    public static NormalSide fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return NormalSide.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Obtiene el lado normal recomendado según el tipo de cuenta.
     *
     * @param accountType Tipo de cuenta
     * @return Lado normal recomendado para ese tipo de cuenta
     */
    public static NormalSide getRecommendedForAccountType(AccountType accountType) {
        if (accountType == null) {
            return null;
        }

        return switch (accountType) {
            case ASSET, EXPENSE -> DEBIT;
            case LIABILITY, EQUITY, REVENUE -> CREDIT;
        };
    }
}
