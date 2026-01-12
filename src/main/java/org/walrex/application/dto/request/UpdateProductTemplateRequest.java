package org.walrex.application.dto.request;

import jakarta.validation.constraints.*;
import org.walrex.domain.model.ProductType;

import java.math.BigDecimal;

/**
 * DTO para actualizar una plantilla de producto existente.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad (internalReference) y existencia de FKs se hace en el Service.
 */
public record UpdateProductTemplateRequest(
    /**
     * Nombre del producto o servicio.
     * - Entre 2 y 255 caracteres
     * - Requerido
     */
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    String name,

    /**
     * Referencia interna única del producto.
     * - Entre 1 y 100 caracteres
     * - Opcional pero recomendado
     * - Se normalizará a mayúsculas automáticamente
     */
    @Size(min = 1, max = 100, message = "La referencia interna debe tener entre 1 y 100 caracteres")
    String internalReference,

    /**
     * Tipo de producto (STORABLE, CONSUMABLE, SERVICE).
     * - Requerido
     */
    @NotNull(message = "El tipo de producto es requerido")
    ProductType type,

    /**
     * ID de la categoría de producto.
     * - Opcional
     */
    Integer categoryId,

    /**
     * ID de la marca del producto.
     * - Opcional
     */
    Integer brandId,

    /**
     * ID de la unidad de medida.
     * - Requerido
     */
    @NotNull(message = "La unidad de medida es requerida")
    Integer uomId,

    /**
     * ID de la moneda para precios.
     * - Requerido
     */
    @NotNull(message = "La moneda es requerida")
    Integer currencyId,

    /**
     * Precio de venta.
     * - Requerido
     * - Debe ser >= 0
     */
    @NotNull(message = "El precio de venta es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio de venta debe ser mayor o igual a 0")
    BigDecimal salePrice,

    /**
     * Costo del producto.
     * - Requerido
     * - Debe ser >= 0
     */
    @NotNull(message = "El costo es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo debe ser mayor o igual a 0")
    BigDecimal cost,

    /**
     * Indica si el producto está exento de IGV/IVA.
     * - Opcional (por defecto false)
     */
    Boolean isIGVExempt,

    /**
     * Tasa de impuesto aplicable.
     * - Requerido
     * - Debe estar entre 0 y 1 (0% a 100%)
     */
    @NotNull(message = "La tasa de impuesto es requerida")
    @DecimalMin(value = "0.0", inclusive = true, message = "La tasa de impuesto debe ser mayor o igual a 0")
    @DecimalMax(value = "1.0", inclusive = true, message = "La tasa de impuesto debe ser menor o igual a 1")
    BigDecimal taxRate,

    /**
     * Peso del producto.
     * - Opcional
     */
    BigDecimal weight,

    /**
     * Volumen del producto.
     * - Opcional
     */
    BigDecimal volume,

    /**
     * Indica si se rastrea inventario para este producto.
     * - Opcional (por defecto true)
     * - Se forzará a false para servicios
     */
    Boolean trackInventory,

    /**
     * Indica si se usan números de serie para este producto.
     * - Opcional (por defecto false)
     * - Se forzará a false para servicios y consumibles
     */
    Boolean useSerialNumbers,

    /**
     * Stock mínimo recomendado.
     * - Opcional
     */
    BigDecimal minimumStock,

    /**
     * Stock máximo recomendado.
     * - Opcional
     */
    BigDecimal maximumStock,

    /**
     * Punto de reorden.
     * - Opcional
     */
    BigDecimal reorderPoint,

    /**
     * Tiempo de entrega en días.
     * - Opcional
     */
    Integer leadTime,

    /**
     * URL o path de la imagen del producto.
     * - Opcional
     */
    String image,

    /**
     * Descripción interna del producto.
     * - Opcional
     */
    String description,

    /**
     * Descripción para ventas/cotizaciones.
     * - Opcional
     */
    String descriptionSale,

    /**
     * Código de barras del producto.
     * - Opcional
     * - Máximo 100 caracteres
     */
    @Size(max = 100, message = "El código de barras debe tener máximo 100 caracteres")
    String barcode,

    /**
     * Notas adicionales.
     * - Opcional
     */
    String notes,

    /**
     * Indica si el producto puede ser vendido.
     * - Opcional (por defecto true)
     */
    Boolean canBeSold,

    /**
     * Indica si el producto puede ser comprado.
     * - Opcional (por defecto true)
     */
    Boolean canBePurchased,

    /**
     * Permite editar el precio durante la venta.
     * - Opcional (por defecto false)
     * - Solo puede ser true para productos de tipo SERVICE
     */
    Boolean allowsPriceEdit,

    /**
     * Indica si el producto tiene variantes.
     * - Opcional (por defecto false)
     */
    Boolean hasVariants,

    /**
     * Estado del producto (active, inactive, discontinued).
     * - Opcional (por defecto active)
     * - Máximo 20 caracteres
     */
    @Size(max = 20, message = "El estado debe tener máximo 20 caracteres")
    String status

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public UpdateProductTemplateRequest {
        // Normalizar strings
        if (name != null) {
            name = name.trim();
        }
        if (internalReference != null && !internalReference.isBlank()) {
            internalReference = internalReference.trim().toUpperCase();
        }
        if (image != null) {
            image = image.trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (descriptionSale != null) {
            descriptionSale = descriptionSale.trim();
        }
        if (barcode != null) {
            barcode = barcode.trim();
        }
        if (notes != null) {
            notes = notes.trim();
        }
        if (status != null) {
            status = status.trim().toLowerCase();
        }

        // Establecer valores por defecto
        if (type == null) {
            type = ProductType.STORABLE;
        }
        if (salePrice == null) {
            salePrice = BigDecimal.ZERO;
        }
        if (cost == null) {
            cost = BigDecimal.ZERO;
        }
        if (isIGVExempt == null) {
            isIGVExempt = false;
        }
        if (taxRate == null) {
            taxRate = new BigDecimal("0.18");
        }
        if (trackInventory == null) {
            trackInventory = true;
        }
        if (useSerialNumbers == null) {
            useSerialNumbers = false;
        }
        if (canBeSold == null) {
            canBeSold = true;
        }
        if (canBePurchased == null) {
            canBePurchased = true;
        }
        if (allowsPriceEdit == null) {
            allowsPriceEdit = false;
        }
        if (hasVariants == null) {
            hasVariants = false;
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
    }
}
