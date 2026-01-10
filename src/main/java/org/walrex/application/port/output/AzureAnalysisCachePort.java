package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.DocumentAnalysisResult;

public interface AzureAnalysisCachePort {

    /**
     * Obtiene el resultado de Azure AI desde cache.
     *
     * @param imageHash Hash SHA-256 de la imagen
     * @return Uni con DocumentAnalysisResult si existe, null si no existe
     */
    Uni<DocumentAnalysisResult> get(String imageHash);

    /**
     * Guarda el resultado de Azure AI en cache.
     *
     * @param imageHash Hash SHA-256 de la imagen
     * @param result Resultado del análisis de Azure
     * @return Uni que se completa cuando se almacena
     */
    Uni<Void> put(String imageHash, DocumentAnalysisResult result);

    /**
     * Invalida el cache de una imagen.
     */
    Uni<Void> invalidate(String imageHash);
}
