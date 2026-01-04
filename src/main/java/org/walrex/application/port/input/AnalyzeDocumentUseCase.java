package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.walrex.domain.model.DocumentAnalysisResult;

/**
 * Puerto de entrada (use case) para analizar documentos con Azure Document Intelligence.
 * Define el contrato para procesar documentos y extraer información estructurada.
 */
public interface AnalyzeDocumentUseCase {

    /**
     * Analiza un documento (PDF o imagen) y extrae su contenido y campos estructurados.
     *
     * @param fileUpload Archivo a analizar
     * @return Uni con el resultado del análisis incluyendo texto y campos del invoice
     */
    Uni<DocumentAnalysisResult> analyzeDocument(FileUpload fileUpload);

    /**
     * Analiza un documento desde un array de bytes.
     *
     * @param documentBytes Contenido del documento en bytes
     * @param contentType   Tipo de contenido (application/pdf, image/jpeg, image/png)
     * @param fileName      Nombre del archivo (para logging/debugging)
     * @return Uni con el resultado del análisis
     */
    Uni<DocumentAnalysisResult> analyzeDocument(byte[] documentBytes, String contentType, String fileName);
}
