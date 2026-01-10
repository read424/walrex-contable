package org.walrex.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.DocumentAnalysisResult;
import org.walrex.domain.model.InvoiceField;

/**
 * Servicio para crear chunks semánticos inteligentes a partir del análisis de Azure AI.
 *
 * Utiliza técnicas de procesamiento de texto para extraer conceptos clave
 * en lugar de simplemente concatenar campos.
*/
@Slf4j
@ApplicationScoped
public class SemanticChunkingService {
    /**
     * Crea un chunk semántico a partir del resultado de Azure AI.
     *
     * Ejemplo de output:
     * "Operación de COMPRA: Venta de 72.83 USDT por Bs 42,945.00 en Binance,
     *  pagado mediante Mercantil el 2026-01-05"
     *
     * @param azureResult Resultado del análisis de Azure Document Intelligence
     * @param bookType Tipo de libro contable (COMPRA, VENTA, DIARIO)
     * @return Texto del chunk semántico
     */
    public String createSemanticChunk(DocumentAnalysisResult azureResult, AccountingBookType bookType) {
        StringBuilder chunk = new StringBuilder();

        // 1. Incluir tipo de operación
        chunk.append("Operación de ").append(bookType.name()).append(": ");

        // 2. Extraer información estructurada si existe
        if (azureResult.getInvoiceFields() != null) {
            InvoiceField invoice = azureResult.getInvoiceFields();

            if (invoice.getVendorName() != null) {
                chunk.append("Proveedor ").append(invoice.getVendorName()).append(", ");
            }

            if (invoice.getTotalAmount() != null) {
                chunk.append("monto ").append(invoice.getTotalAmount()).append(", ");
            }

            if (invoice.getInvoiceDate() != null) {
                chunk.append("fecha ").append(invoice.getInvoiceDate()).append(". ");
            }
        }

        // 3. Extraer keywords del contenido usando análisis semántico
        if (azureResult.getContent() != null && !azureResult.getContent().isBlank()) {
            String enrichedContent = extractKeyConceptsFromContent(azureResult.getContent());
            chunk.append(enrichedContent);
        }

        String result = chunk.toString().trim();
        log.debug("Created semantic chunk: {}", result);
        return result;
    }

    /**
     * Extrae conceptos clave del texto crudo de Azure AI.
     *
     * Identifica patrones como:
     * - "Sell USDT" / "Buy BTC" → Operación con criptoactivo
     * - "Fiat amount Bs X" → Monto en bolívares
     * - "Payment method Mercantil" → Método de pago
     * - Cantidades numéricas relevantes
     */
    private String extractKeyConceptsFromContent(String content) {
        StringBuilder concepts = new StringBuilder();

        // Detectar operaciones con criptoactivos
        if (content.contains("Sell") || content.contains("Buy")) {
            String operation = content.contains("Sell") ? "Venta" : "Compra";

            // Extraer asset (USDT, BTC, etc.)
            String asset = extractAsset(content);
            if (asset != null) {
                concepts.append(operation).append(" de ").append(asset).append(". ");
            }
        }

        // Extraer cantidad
        String quantity = extractQuantity(content);
        if (quantity != null) {
            concepts.append("Cantidad: ").append(quantity).append(". ");
        }

        // Extraer monto fiat
        String fiatAmount = extractFiatAmount(content);
        if (fiatAmount != null) {
            concepts.append("Monto: ").append(fiatAmount).append(". ");
        }

        // Extraer método de pago
        String paymentMethod = extractPaymentMethod(content);
        if (paymentMethod != null) {
            concepts.append("Pagado mediante ").append(paymentMethod).append(". ");
        }

        // Si no se encontraron conceptos específicos, tomar primeras 200 chars
        if (concepts.length() == 0) {
            String truncated = content.length() > 200 ?
                    content.substring(0, 200) + "..." : content;
            concepts.append(truncated);
        }

        return concepts.toString();
    }

    private String extractAsset(String content) {
        // Buscar patrones como "72.83 USDT", "0.5 BTC"
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.matches(".*\\d+\\.?\\d*\\s+(USDT|BTC|ETH|BNB).*")) {
                String[] parts = line.trim().split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i + 1].matches("USDT|BTC|ETH|BNB")) {
                        return parts[i] + " " + parts[i + 1];
                    }
                }
            }
        }
        return null;
    }

    private String extractQuantity(String content) {
        // Buscar "Total Quantity" seguido de número
        if (content.contains("Total Quantity")) {
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("Total Quantity")) {
                    if (i + 1 < lines.length) {
                        return lines[i + 1].trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractFiatAmount(String content) {
        // Buscar "Fiat amount" seguido de moneda y monto
        if (content.contains("Fiat amount")) {
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("Fiat amount")) {
                    if (i + 1 < lines.length) {
                        return lines[i + 1].trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractPaymentMethod(String content) {
        // Buscar "Payment method" seguido del nombre
        if (content.contains("Payment method")) {
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("Payment method")) {
                    if (i + 1 < lines.length) {
                        return lines[i + 1].trim();
                    }
                }
            }
        }
        return null;
    }
}
