package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductVariant;

/**
 * Puerto de salida para operaciones de persistencia de ProductVariant.
 *
 * Define el contrato que debe cumplir cualquier adaptador de persistencia
 * para ProductVariant.
 *
 * NOTA: Este es un puerto MINIMAL con solo la operación save() necesaria
 * para que ProductTemplateService cree variantes de productos automáticamente.
 * ProductVariant se gestiona internamente y no requiere operaciones CRUD completas.
 */
public interface ProductVariantRepositoryPort {

    /**
     * Persiste una nueva variante de producto.
     *
     * @param productVariant Entidad de dominio a persistir
     * @return Uni con la variante persistida incluyendo el ID generado
     */
    Uni<ProductVariant> save(ProductVariant productVariant);
}
