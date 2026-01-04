package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.DocumentAnalysisResult;

/**
 * Puerto de salida para la comunicación con Azure Document Intelligence.
 * Este puerto será implementado por un adapter en la capa de infraestructura.
 */
public interface DocumentIntelligencePort {

    /**
     * Analiza un documento usando Azure Document Intelligence API.
     *
     * @param documentBytes Contenido del documento en bytes
     * @param contentType   Tipo de contenido (application/pdf, image/jpeg, image/png)
     * @return Uni con el resultado del análisis
     */
    Uni<DocumentAnalysisResult> analyzeInvoice(byte[] documentBytes, String contentType);
}
