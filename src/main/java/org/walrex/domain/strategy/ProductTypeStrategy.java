package org.walrex.domain.strategy;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.domain.model.ProductVariant;

/**
 * Estrategia para manejar lógica específica de cada tipo de producto.
 *
 * Implementa el patrón Strategy para evitar condicionales complejos
 * y facilitar la extensión del sistema con nuevos tipos de producto.
 *
 * Cada implementación concreta (ServiceProductStrategy, StorableProductStrategy, etc.)
 * encapsula la lógica específica de ese tipo.
 */
public interface ProductTypeStrategy {

    /**
     * Aplica las reglas de negocio específicas del tipo de producto.
     *
     * Ejemplos:
     * - Servicios: Desactivar inventario, limpiar propiedades físicas
     * - Consumibles: Desactivar números de serie
     * - Almacenables: Validar stock mínimo/máximo
     *
     * @param template Plantilla de producto a modificar
     */
    void applyTypeSpecificRules(ProductTemplate template);

    /**
     * Valida que los datos del producto sean consistentes con su tipo.
     *
     * Ejemplos:
     * - Servicios: No deben tener allowsPriceEdit=true
     * - Almacenables: Deben tener UOM de almacén
     *
     * @param template Plantilla a validar
     * @return Uni que completa si es válido, falla con excepción si no
     */
    Uni<Void> validateBusinessRules(ProductTemplate template);

    /**
     * Determina si este tipo de producto puede tener variantes.
     *
     * @return true si el tipo soporta variantes (atributos configurables)
     */
    boolean supportsVariants();

    /**
     * Determina si este tipo de producto requiere crear variante por defecto.
     *
     * @param template Plantilla del producto
     * @return true si debe crear variante por defecto automáticamente
     */
    boolean requiresDefaultVariant(ProductTemplate template);

    /**
     * Crea la variante por defecto para este tipo de producto.
     *
     * @param template Plantilla del producto
     * @return Uni con la variante creada, o null si no aplica
     */
    Uni<ProductVariant> createDefaultVariant(ProductTemplate template);
}
