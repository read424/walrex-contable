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
            basados en documentos de facturas, comprobantes y operaciones de criptomonedas.

            Tu tarea es analizar la información proporcionada y sugerir las cuentas contables
            apropiadas con sus débitos y créditos, asegurando que el asiento esté balanceado.

            REGLAS IMPORTANTES:
            1. El total de débitos DEBE ser igual al total de créditos
            2. Usa las cuentas del plan de cuentas proporcionado en el contexto
            3. Aprende de los asientos históricos similares proporcionados
            4. Responde ÚNICAMENTE en formato JSON estructurado
            5. No inventes códigos de cuenta, usa solo los proporcionados

            OPERACIONES DE CRIPTOMONEDAS (Binance P2P, etc.):
            - COMPRA de cripto: Débito = Criptoactivo (1611), Crédito = Banco/Efectivo
            - VENTA de cripto: Débito = Banco/Efectivo o Cuentas por Cobrar, Crédito = Criptoactivo (1611)
            - Siempre incluye la cantidad de cripto en la descripción (ej: "Compra 23.52 USDT")
            - Método de pago relevante (Yape, Bank Transfer, etc.)

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

        // Detectar si es un documento de Binance
        CryptoOperationInfo cryptoInfo = detectCryptoOperation(context);

        // Información del documento
        if (cryptoInfo != null && cryptoInfo.isBinance) {
            // Prompt especializado para Binance
            prompt.append(buildBinanceDocumentSection(cryptoInfo));
        } else if (context.getDocumentAnalysis() != null && context.getDocumentAnalysis().getInvoiceFields() != null) {
            // Prompt genérico para facturas
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
        if (cryptoInfo != null && cryptoInfo.isBinance) {
            prompt.append(String.format("Genera un asiento contable para registrar esta %s de criptomoneda.\n",
                    cryptoInfo.isBuy ? "COMPRA" : "VENTA"));
            prompt.append(String.format("Operación: %s %s USDT\n",
                    cryptoInfo.isBuy ? "Compra de" : "Venta de", cryptoInfo.cryptoAmount));
            prompt.append("Usa las cuentas del plan proporcionado y ESPECIALMENTE aprende de los asientos históricos de USDT.\n");
        } else {
            prompt.append("Genera un asiento contable para registrar esta factura de compra.\n");
            prompt.append("Usa las cuentas del plan proporcionado y aprende de los asientos históricos.\n");
        }
        prompt.append("Responde en formato JSON como se especificó en el system prompt.");

        return prompt.toString();
    }

    /**
     * Detecta si el documento es una operación de criptomonedas (Binance).
     */
    private CryptoOperationInfo detectCryptoOperation(RAGContext context) {
        if (context.getDocumentAnalysis() == null || context.getDocumentAnalysis().getContent() == null) {
            return null;
        }

        String content = context.getDocumentAnalysis().getContent().toLowerCase();

        // Detectar Binance
        boolean isBinance = content.contains("binance") ||
                content.contains("order completed") ||
                content.contains("buy usdt") ||
                content.contains("sell usdt");

        if (!isBinance) {
            return null;
        }

        CryptoOperationInfo info = new CryptoOperationInfo();
        info.isBinance = true;

        // Detectar tipo de operación
        info.isBuy = content.contains("buy usdt") || content.contains("buy") && content.contains("usdt");
        info.isSell = content.contains("sell usdt") || content.contains("sell") && content.contains("usdt");

        // Intentar extraer cantidad de USDT
        info.cryptoAmount = extractCryptoAmount(content);

        // Intentar extraer monto fiat
        info.fiatAmount = extractFiatAmount(context);

        // Intentar extraer método de pago
        info.paymentMethod = extractPaymentMethod(content);

        log.info("Detected Binance operation: Buy={}, Sell={}, Amount={} USDT, Fiat={}, Method={}",
                info.isBuy, info.isSell, info.cryptoAmount, info.fiatAmount, info.paymentMethod);

        return info;
    }

    /**
     * Extrae la cantidad de USDT del contenido.
     */
    private String extractCryptoAmount(String content) {
        // Buscar patrones como "23.52 USDT" o "Receive Quantity 23.52"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+\\.\\d+)\\s*usdt|receive quantity\\s*(\\d+\\.\\d+)|total quantity\\s*(\\d+\\.\\d+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return matcher.group(i);
                }
            }
        }

        return "cantidad no detectada";
    }

    /**
     * Extrae el monto fiat del análisis del documento.
     */
    private String extractFiatAmount(RAGContext context) {
        if (context.getDocumentAnalysis() != null &&
                context.getDocumentAnalysis().getInvoiceFields() != null &&
                context.getDocumentAnalysis().getInvoiceFields().getTotalAmount() != null) {

            String amount = context.getDocumentAnalysis().getInvoiceFields().getTotalAmount().toString();
            String currency = context.getDocumentAnalysis().getInvoiceFields().getCurrencyCode();

            return currency != null ? amount + " " + currency : amount;
        }

        return null;
    }

    /**
     * Extrae el método de pago del contenido.
     */
    private String extractPaymentMethod(String content) {
        if (content.contains("yape")) {
            return "Yape";
        } else if (content.contains("bank transfer")) {
            return "Bank Transfer";
        } else if (content.contains("transferencia")) {
            return "Transferencia Bancaria";
        }
        return "No especificado";
    }

    /**
     * Construye la sección del documento para operaciones de Binance.
     */
    private String buildBinanceDocumentSection(CryptoOperationInfo info) {
        StringBuilder section = new StringBuilder();

        section.append("## DOCUMENTO A PROCESAR\n");
        section.append("Tipo: OPERACIÓN DE CRIPTOMONEDA (Binance P2P)\n");
        section.append("Plataforma: Binance\n");

        if (info.isBuy) {
            section.append("Operación: COMPRA de USDT\n");
            section.append(String.format("Cantidad adquirida: %s USDT\n", info.cryptoAmount));
        } else if (info.isSell) {
            section.append("Operación: VENTA de USDT\n");
            section.append(String.format("Cantidad vendida: %s USDT\n", info.cryptoAmount));
        }

        if (info.fiatAmount != null) {
            section.append(String.format("Monto en moneda fiat: %s\n", info.fiatAmount));
        }

        if (info.paymentMethod != null) {
            section.append(String.format("Método de pago: %s\n", info.paymentMethod));
        }

        section.append("\nCONTEXTO CONTABLE ESPECÍFICO:\n");
        if (info.isBuy) {
            section.append("- Esta es una COMPRA de criptoactivo (USDT)\n");
            section.append("- Se debe DEBITAR la cuenta de Criptoactivos (código 1611)\n");
            section.append("- Se debe ACREDITAR la cuenta de Banco/Efectivo usada para el pago\n");
            section.append("- El asiento debe reflejar el AUMENTO del activo cripto y DISMINUCIÓN de efectivo/banco\n");
        } else if (info.isSell) {
            section.append("- Esta es una VENTA de criptoactivo (USDT)\n");
            section.append("- Se debe DEBITAR la cuenta de Banco/Efectivo o Cuentas por Cobrar\n");
            section.append("- Se debe ACREDITAR la cuenta de Criptoactivos (código 1611)\n");
            section.append("- El asiento debe reflejar la DISMINUCIÓN del activo cripto y AUMENTO de efectivo/cuentas por cobrar\n");
        }

        section.append("\n");
        return section.toString();
    }

    /**
     * Clase interna para almacenar información de operaciones cripto.
     */
    private static class CryptoOperationInfo {
        boolean isBinance = false;
        boolean isBuy = false;
        boolean isSell = false;
        String cryptoAmount;
        String fiatAmount;
        String paymentMethod;
    }
}
