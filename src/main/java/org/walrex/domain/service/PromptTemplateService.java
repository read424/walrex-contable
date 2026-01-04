package org.walrex.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.model.*;

/**
 * Servicio para generar prompts estructurados para el LLM.
 */
@Slf4j
@ApplicationScoped
public class PromptTemplateService {

    /**
     * Genera el system prompt para el LLM.
     */
    public String buildSystemPrompt() {
        return """
            Eres un asistente experto en contabilidad que ayuda a generar asientos contables
            basados en documentos de facturas y comprobantes.

            Tu tarea es analizar la información proporcionada y sugerir las cuentas contables
            apropiadas con sus débitos y créditos, asegurando que el asiento esté balanceado.

            REGLAS IMPORTANTES:
            1. El total de débitos DEBE ser igual al total de créditos
            2. Usa las cuentas del plan de cuentas proporcionado en el contexto
            3. Aprende de los asientos históricos similares proporcionados
            4. Responde ÚNICAMENTE en formato JSON estructurado
            5. No inventes códigos de cuenta, usa solo los proporcionados

            Formato de respuesta JSON:
            {
              "description": "Descripción del asiento",
              "lines": [
                {
                  "accountId": 123,
                  "accountCode": "1010",
                  "accountName": "Caja",
                  "debit": 1000.00,
                  "credit": 0.00,
                  "description": "Descripción de la línea",
                  "explanation": "Por qué se usa esta cuenta"
                }
              ],
              "explanation": "Explicación general del asiento",
              "confidence": 0.85
            }
            """;
    }

    /**
     * Genera el user prompt con el contexto del documento y resultados de búsqueda.
     */
    public String buildUserPrompt(RAGContext context, RetrievedContext retrievedContext) {
        StringBuilder prompt = new StringBuilder();

        // Información del documento
        if (context.getDocumentAnalysis() != null && context.getDocumentAnalysis().getInvoiceFields() != null) {
            InvoiceField invoice = context.getDocumentAnalysis().getInvoiceFields();
            prompt.append("## DOCUMENTO A PROCESAR\n");
            prompt.append("Tipo: Factura\n");

            if (invoice.getVendorName() != null) {
                prompt.append(String.format("Proveedor: %s\n", invoice.getVendorName()));
            }
            if (invoice.getInvoiceDate() != null) {
                prompt.append(String.format("Fecha: %s\n", invoice.getInvoiceDate()));
            }
            if (invoice.getInvoiceId() != null) {
                prompt.append(String.format("Número: %s\n", invoice.getInvoiceId()));
            }
            if (invoice.getTotalAmount() != null) {
                prompt.append(String.format("Monto Total: %s", invoice.getTotalAmount()));
                if (invoice.getCurrencyCode() != null) {
                    prompt.append(String.format(" %s", invoice.getCurrencyCode()));
                }
                prompt.append("\n");
            }
            if (invoice.getSubtotal() != null) {
                prompt.append(String.format("Subtotal: %s\n", invoice.getSubtotal()));
            }
            if (invoice.getTotalTax() != null) {
                prompt.append(String.format("Impuestos: %s\n", invoice.getTotalTax()));
            }
            prompt.append("\n");
        }

        // Cuentas contables recuperadas
        if (retrievedContext.getSimilarAccounts() != null && !retrievedContext.getSimilarAccounts().isEmpty()) {
            prompt.append("## CUENTAS CONTABLES DISPONIBLES (Plan de Cuentas)\n");
            for (AccountSearchResult account : retrievedContext.getSimilarAccounts()) {
                prompt.append(String.format("- [%s] %s (Tipo: %s, Naturaleza: %s, Score: %.2f)\n",
                        account.getCode(), account.getName(),
                        account.getType(), account.getNormalSide(), account.getScore()));
            }
            prompt.append("\n");
        }

        // Asientos históricos similares
        if (retrievedContext.getSimilarHistoricalEntries() != null &&
                !retrievedContext.getSimilarHistoricalEntries().isEmpty()) {
            prompt.append("## ASIENTOS HISTÓRICOS SIMILARES (Para aprender el patrón)\n");
            for (HistoricalEntryChunk entry : retrievedContext.getSimilarHistoricalEntries()) {
                prompt.append(String.format("- Fecha: %s | %s (Score: %.2f)\n",
                        entry.getEntryDate(), entry.getDescription(), entry.getSimilarityScore()));
                prompt.append(String.format("  %s\n", entry.getChunkText()));
            }
            prompt.append("\n");
        }

        // Instrucción final
        prompt.append("## INSTRUCCIÓN\n");
        prompt.append("Genera un asiento contable para registrar esta factura de compra.\n");
        prompt.append("Usa las cuentas del plan proporcionado y aprende de los asientos históricos.\n");
        prompt.append("Responde en formato JSON como se especificó en el system prompt.");

        return prompt.toString();
    }
}
