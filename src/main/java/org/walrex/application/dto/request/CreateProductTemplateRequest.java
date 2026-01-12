package org.walrex.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.walrex.domain.model.ProductType;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para crear una nueva plantilla de producto.
 *
 * Las validaciones aquí son de FORMATO, no de negocio.
 * La validación de unicidad (internalReference) y existencia de FKs se hace en el Service.
 */
public record CreateProductTemplateRequest(
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
     * - Opcional (validación según tipo se hace en estrategia)
     */
    Integer uomId,

    /**
     * ID de la moneda para precios.
     * - Opcional (validación según tipo se hace en estrategia)
     */
    Integer currencyId,

    /**
     * Precio de venta.
     * - Opcional (validación según tipo se hace en estrategia)
     * - Si se provee, debe ser >= 0
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio de venta debe ser mayor o igual a 0")
    BigDecimal salePrice,

    /**
     * Costo del producto.
     * - Opcional (validación según tipo se hace en estrategia)
     * - Si se provee, debe ser >= 0
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo debe ser mayor o igual a 0")
    BigDecimal cost,

    /**
     * Indica si el producto está exento de IGV/IVA.
     * - Opcional (por defecto false)
     */
    Boolean isIGVExempt,

    /**
     * Tasa de impuesto aplicable.
     * - Opcional (validación según tipo se hace en estrategia)
     * - Si se provee, debe estar entre 0 y 1 (0% a 100%)
     */
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
    String status,

    /**
     * IDs de los atributos para productos con variantes configurables.
     * - Opcional (null para productos simples sin variantes configurables)
     * - Requerido si variants está presente
     * - Solo aplicable para STORABLE y CONSUMABLE
     */
    List<Integer> attributeIds,

    /**
     * Lista de variantes para productos con variantes configurables.
     * - Opcional (null para productos simples sin variantes configurables)
     * - Requerido si attributeIds está presente
     * - Solo aplicable para STORABLE y CONSUMABLE
     */
    @Valid
    List<VariantRequest> variants

) {
    /**
     * Constructor compacto que normaliza los datos.
     */
    public CreateProductTemplateRequest {
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

        // Establecer valores por defecto solo para campos críticos
        // Los demás se validan en las estrategias según el tipo de producto
        if (type == null) {
            type = ProductType.STORABLE;
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
        if (isIGVExempt == null) {
            isIGVExempt = false;
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

        // Validar campos de variantes configurables
        // Solo considerar "presente" si no es null Y no está vacío
        boolean hasAttributes = attributeIds != null && !attributeIds.isEmpty();
        boolean hasVariantsData = variants != null && !variants.isEmpty();

        // Si uno está presente, ambos deben estarlo
        if (hasAttributes != hasVariantsData) {
            // Solo lanzar excepción si realmente uno tiene datos y el otro no
            // Arrays vacíos o null se consideran "ausente"
            if (hasAttributes) {
                throw new IllegalArgumentException(
                        "Si se especifican attributeIds, también se deben especificar variants. " +
                        "Para productos con variantes configurables, ambos campos son requeridos.");
            }
            if (hasVariantsData) {
                throw new IllegalArgumentException(
                        "Si se especifican variants, también se deben especificar attributeIds. " +
                        "Para productos con variantes configurables, ambos campos son requeridos.");
            }
        }

        // Si tiene variantes configurables, actualizar hasVariants a true
        if (hasAttributes && hasVariantsData) {
            hasVariants = true;
        }
    }
}
