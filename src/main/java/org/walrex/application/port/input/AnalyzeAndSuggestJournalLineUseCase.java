package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.walrex.application.dto.response.JournalLineSuggestionResponse;

import java.util.List;

/**
 * Caso de uso para analizar un documento (imagen/PDF) y sugerir líneas de asiento contable.
 *
 * Flujo completo:
 * 1. Analiza el documento con Azure Document Intelligence
 * 2. Usa RAG Orchestrator para buscar cuentas y generar sugerencias
 * 3. Retorna las líneas sugeridas en formato simplificado
 */
public interface AnalyzeAndSuggestJournalLineUseCase {

    /**
     * Analiza un documento subido y genera sugerencias de líneas de asiento.
     *
     * @param fileUpload Archivo subido (imagen o PDF)
     * @param bookType Tipo de libro contable (opcional, ej: "DIARIO", "COMPRAS", "VENTAS")
     * @return Lista de líneas de asiento sugeridas
     */
    Uni<List<JournalLineSuggestionResponse>> analyzeAndSuggest(
            FileUpload fileUpload,
            String bookType
    );
}
