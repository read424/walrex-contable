package org.walrex.application.dto.query;

import lombok.Builder;
import lombok.Data;
import org.walrex.domain.model.ProductType;

/**
 * Filtro para búsqueda de plantillas de producto.
 *
 * Permite filtrar por múltiples criterios:
 * - Búsqueda general en nombre, referencia interna, descripción
 * - Filtros específicos por nombre, referencia, tipo, categoría, marca, estado
 * - Filtros booleanos: canBeSold, canBePurchased, hasVariants, active
 * - Opción para incluir registros eliminados
 */
@Data
@Builder
public class ProductTemplateFilter {

    /**
     * Búsqueda general en nombre, referencia interna, descripción
     */
    private String search;

    /**
     * Filtro por nombre exacto
     */
    private String name;

    /**
     * Filtro por referencia interna exacta
     */
    private String internalReference;

    /**
     * Filtro por tipo de producto
     */
    private ProductType type;

    /**
     * Filtro por categoría
     */
    private Integer categoryId;

    /**
     * Filtro por marca
     */
    private Integer brandId;

    /**
     * Filtro por estado (active, inactive, discontinued)
     */
    private String status;

    /**
     * Filtro por productos que pueden ser vendidos
     */
    private String canBeSold;

    /**
     * Filtro por productos que pueden ser comprados
     */
    private String canBePurchased;

    /**
     * Filtro por productos con variantes
     */
    private String hasVariants;

    /**
     * Filtro por estado activo/inactivo
     */
    private String active;

    /**
     * Incluir registros eliminados ("1" = incluir, otro valor = excluir)
     */
    private String includeDeleted;
}
