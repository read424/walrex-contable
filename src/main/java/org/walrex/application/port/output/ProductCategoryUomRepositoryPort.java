package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductCategoryUom;

/**
 * Puerto de salida para operaciones de escritura en categorías de unidades de medida.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de persistencia sin especificar la tecnología (PostgreSQL, etc.).
 */
public interface ProductCategoryUomRepositoryPort {

    /**
     * Persiste una nueva categoría de unidad de medida.
     *
     * SQL esperado:
     * INSERT INTO product_category_uom (...) VALUES ($1, $2, ...) RETURNING *
     *
     * @param productCategoryUom Entidad de dominio a persistir
     * @return Uni con la categoría persistida (incluye timestamps del servidor)
     */
    Uni<ProductCategoryUom> save(ProductCategoryUom productCategoryUom);

    /**
     * Actualiza una categoría existente.
     *
     * SQL esperado:
     * UPDATE product_category_uom SET code=$1, name=$2, ... , updated_at=NOW()
     * WHERE id=$n AND deleted_at IS NULL RETURNING *
     *
     * @param productCategoryUom Entidad de dominio con los datos actualizados
     * @return Uni con la categoría actualizada
     */
    Uni<ProductCategoryUom> update(ProductCategoryUom productCategoryUom);

    /**
     * Elimina lógicamente una categoría (soft delete).
     *
     * SQL esperado:
     * UPDATE product_category_uom SET deleted_at=NOW(), is_active=false, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identificador de la categoría
     * @return Uni<Boolean> true si se eliminó (rowCount > 0), false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente una categoría (hard delete).
     *
     * SQL esperado:
     * DELETE FROM product_category_uom WHERE id=$1
     *
     * ⚠️ Usar con precaución. Preferir softDelete.
     *
     * @param id Identificador de la categoría
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura una categoría previamente eliminada.
     *
     * SQL esperado:
     * UPDATE product_category_uom SET deleted_at=NULL, is_active=true, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identificador de la categoría
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminada
     */
    Uni<Boolean> restore(Integer id);
}
