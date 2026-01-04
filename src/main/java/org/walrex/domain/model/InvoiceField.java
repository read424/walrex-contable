package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa los campos estructurados extraídos de un documento tipo invoice.
 * Contiene información específica de facturas/comprobantes como importes, fechas, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceField {

    /**
     * Número de factura/comprobante.
     */
    private String invoiceId;

    /**
     * Fecha de emisión del documento.
     */
    private LocalDate invoiceDate;

    /**
     * Fecha de vencimiento (si aplica).
     */
    private LocalDate dueDate;

    /**
     * Nombre del proveedor/vendedor.
     */
    private String vendorName;

    /**
     * Dirección del proveedor.
     */
    private String vendorAddress;

    /**
     * Nombre del cliente/comprador.
     */
    private String customerName;

    /**
     * Dirección del cliente.
     */
    private String customerAddress;

    /**
     * Subtotal (antes de impuestos).
     */
    private BigDecimal subtotal;

    /**
     * Total de impuestos.
     */
    private BigDecimal totalTax;

    /**
     * Importe total del documento.
     */
    private BigDecimal totalAmount;

    /**
     * Monto pagado anteriormente (si aplica).
     */
    private BigDecimal amountPaid;

    /**
     * Balance pendiente de pago.
     */
    private BigDecimal amountDue;

    /**
     * Código de moneda (USD, PEN, etc.).
     */
    private String currencyCode;

    /**
     * Nivel de confianza general de los campos extraídos (0.0 a 1.0).
     */
    private Double confidence;
}
