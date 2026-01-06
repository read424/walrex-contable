package org.walrex.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de respuesta para sugerencia de línea de asiento contable.
 * Formato simplificado para el frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalLineSuggestionResponse {

    /**
     * ID de la cuenta contable sugerida.
     */
    @JsonProperty("accountId")
    private Integer accountId;

    /**
     * Descripción de la línea sugerida.
     */
    @JsonProperty("description")
    private String description;

    /**
     * Monto débito sugerido.
     */
    @JsonProperty("debit")
    private BigDecimal debit;

    /**
     * Monto crédito sugerido.
     */
    @JsonProperty("credit")
    private BigDecimal credit;

    /**
     * Score de confianza de esta sugerencia (0.0 a 1.0).
     * Opcional, puede ser útil para el frontend.
     */
    @JsonProperty("confidence")
    private Float confidence;
}
