package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un chunk (fragmento de texto) generado a partir de una cuenta contable.
 * Este chunk se usará para generar embeddings y almacenarlos en la base de datos vectorial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountChunk {

    /**
     * ID de la cuenta contable asociada.
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
     * Texto del chunk formateado para embeddings.
     * Formato: "Código: [code]. Cuenta: [name]. Tipo: [type]. Naturaleza: [normal_side].
     * Descripción: Esta cuenta se utiliza para registrar [name] de naturaleza [normal_side]."
     */
    private String chunkText;

    /**
     * Embedding vectorial generado a partir del chunkText.
     * Array de floats con dimensión 1024 (mxbai-embed-large).
     */
    private float[] embedding;

    /**
     * Indica si la cuenta está activa.
     */
    private Boolean active;
}
