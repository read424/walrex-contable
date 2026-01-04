package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de una búsqueda semántica de cuentas contables en la base de datos vectorial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSearchResult {

    /**
     * ID de la cuenta contable encontrada.
     */
    private Integer accountId;

    /**
     * Código de la cuenta contable.
     */
    private String code;

    /**
     * Nombre de la cuenta contable.
     */
    private String name;

    /**
     * Tipo de cuenta contable.
     */
    private AccountType type;

    /**
     * Lado normal de la cuenta.
     */
    private NormalSide normalSide;

    /**
     * Puntuación de similitud (0.0 a 1.0).
     * Valores más altos indican mayor similitud con la consulta.
     */
    private Float score;

    /**
     * Indica si la cuenta está activa.
     */
    private Boolean active;
}
