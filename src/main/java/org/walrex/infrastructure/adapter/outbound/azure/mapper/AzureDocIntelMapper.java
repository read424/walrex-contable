package org.walrex.infrastructure.adapter.outbound.azure.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.DocumentAnalysisResult;
import org.walrex.domain.model.InvoiceField;
import org.walrex.infrastructure.adapter.outbound.azure.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper de MapStruct para convertir DTOs de Azure Document Intelligence
 * a modelos de dominio.
 */
@Mapper(componentModel = "jakarta", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AzureDocIntelMapper {

    /**
     * Convierte la respuesta completa de Azure a modelo de dominio.
     */
    @Mapping(target = "content", source = "analyzeResult.content")
    @Mapping(target = "invoiceFields", source = "analyzeResult.documents", qualifiedByName = "mapInvoiceFields")
    @Mapping(target = "pageCount", source = "analyzeResult.pages", qualifiedByName = "getPageCount")
    @Mapping(target = "modelId", source = "analyzeResult.modelId")
    @Mapping(target = "successful", source = "status", qualifiedByName = "isSuccessful")
    @Mapping(target = "errorMessage", source = "error.message")
    DocumentAnalysisResult toDomain(AzureAnalyzeResponse response);

    /**
     * Extrae los campos del invoice del primer documento encontrado.
     */
    @Named("mapInvoiceFields")
    default InvoiceField mapInvoiceFields(java.util.List<AzureDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }

        AzureDocument firstDoc = documents.get(0);
        if (firstDoc.getFields() == null) {
            return null;
        }

        Map<String, AzureField> fields = firstDoc.getFields();

        return InvoiceField.builder()
                .invoiceId(extractString(fields, "InvoiceId"))
                .invoiceDate(extractDate(fields, "InvoiceDate"))
                .dueDate(extractDate(fields, "DueDate"))
                .vendorName(extractString(fields, "VendorName"))
                .vendorAddress(extractString(fields, "VendorAddress"))
                .customerName(extractString(fields, "CustomerName"))
                .customerAddress(extractString(fields, "CustomerAddress"))
                .subtotal(extractCurrency(fields, "SubTotal"))
                .totalTax(extractCurrency(fields, "TotalTax"))
                .totalAmount(extractCurrency(fields, "InvoiceTotal"))
                .amountPaid(extractCurrency(fields, "AmountPaid"))
                .amountDue(extractCurrency(fields, "AmountDue"))
                .currencyCode(extractCurrencyCode(fields, "InvoiceTotal"))
                .confidence(firstDoc.getConfidence())
                .build();
    }

    /**
     * Cuenta el número de páginas.
     */
    @Named("getPageCount")
    default Integer getPageCount(java.util.List<AzurePageInfo> pages) {
        return pages != null ? pages.size() : 0;
    }

    /**
     * Determina si el análisis fue exitoso.
     */
    @Named("isSuccessful")
    default Boolean isSuccessful(String status) {
        return "succeeded".equalsIgnoreCase(status);
    }

    /**
     * Extrae un valor string de un campo.
     */
    default String extractString(Map<String, AzureField> fields, String fieldName) {
        AzureField field = fields.get(fieldName);
        if (field == null) {
            return null;
        }

        if (field.getValueString() != null) {
            return field.getValueString();
        }

        return field.getContent();
    }

    /**
     * Extrae un valor de fecha de un campo.
     */
    default LocalDate extractDate(Map<String, AzureField> fields, String fieldName) {
        AzureField field = fields.get(fieldName);
        if (field == null || field.getValueDate() == null) {
            return null;
        }

        try {
            // Azure retorna fechas en formato ISO (yyyy-MM-dd)
            return LocalDate.parse(field.getValueDate(), DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrae un valor de moneda de un campo.
     */
    default BigDecimal extractCurrency(Map<String, AzureField> fields, String fieldName) {
        AzureField field = fields.get(fieldName);
        if (field == null) {
            return null;
        }

        // Preferir valueCurrency si está disponible
        if (field.getValueCurrency() != null && field.getValueCurrency().getAmount() != null) {
            return field.getValueCurrency().getAmount();
        }

        // Sino, intentar con valueNumber
        if (field.getValueNumber() != null) {
            return BigDecimal.valueOf(field.getValueNumber());
        }

        return null;
    }

    /**
     * Extrae el código de moneda de un campo.
     */
    default String extractCurrencyCode(Map<String, AzureField> fields, String fieldName) {
        AzureField field = fields.get(fieldName);
        if (field == null || field.getValueCurrency() == null) {
            return null;
        }

        return field.getValueCurrency().getCurrencyCode();
    }
}
