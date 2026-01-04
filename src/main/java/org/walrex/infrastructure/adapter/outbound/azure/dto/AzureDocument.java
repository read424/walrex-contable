package org.walrex.infrastructure.adapter.outbound.azure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * DTO que representa un documento (invoice) extraído por Azure Document Intelligence.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureDocument {

    /**
     * Tipo de documento (invoice).
     */
    private String docType;

    /**
     * Nivel de confianza general del documento.
     */
    private Double confidence;

    /**
     * Campos extraídos del documento.
     * La clave es el nombre del campo (InvoiceId, InvoiceDate, etc.)
     * El valor es un objeto AzureField con el valor y confianza.
     */
    private Map<String, AzureField> fields;
}
