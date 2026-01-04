package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO que representa información de una página del documento.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzurePageInfo {

    /**
     * Número de página (1-based).
     */
    private Integer pageNumber;

    /**
     * Ángulo de rotación de la página.
     */
    private Double angle;

    /**
     * Ancho de la página.
     */
    private Double width;

    /**
     * Alto de la página.
     */
    private Double height;

    /**
     * Unidad de medida (pixel, inch).
     */
    private String unit;
}
