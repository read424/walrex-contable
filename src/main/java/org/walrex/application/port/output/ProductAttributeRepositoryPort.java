package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttribute;

/**
 * Puerto de salida para operaciones de escritura en atributos de producto.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de persistencia sin especificar la tecnología (PostgreSQL, etc.).
 *
 * NOTA: Este puerto usa Integer como tipo de ID (auto-generado).
 */
public interface ProductAttributeRepositoryPort {

    /**
     * Persiste un nuevo atributo de producto.
     *
     * SQL esperado:
     * INSERT INTO product_attributes (name, display_type, is_active, created_at, updated_at)
     * VALUES ($1, $2, $3, $4, $5) RETURNING *
     *
     * @param productAttribute Entidad de dominio a persistir
     * @return Uni con el atributo persistido (incluye timestamps e id auto-generado)
     */
    Uni<ProductAttribute> save(ProductAttribute productAttribute);

    /**
     * Actualiza un atributo existente.
     *
     * SQL esperado:
     * UPDATE product_attributes SET name=$1, display_type=$2, is_active=$3, updated_at=NOW()
     * WHERE id=$4 AND deleted_at IS NULL RETURNING *
     *
     * @param productAttribute Entidad de dominio con los datos actualizados
     * @return Uni con el atributo actualizado
     */
    Uni<ProductAttribute> update(ProductAttribute productAttribute);

    /**
     * Elimina lógicamente un atributo (soft delete).
     *
     * SQL esperado:
     * UPDATE product_attributes SET deleted_at=NOW(), is_active=false, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NULL
     *
     * @param id Identificador del atributo (Integer)
     * @return Uni<Boolean> true si se eliminó (rowCount > 0), false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente un atributo (hard delete).
     *
     * SQL esperado:
     * DELETE FROM product_attributes WHERE id=$1
     *
     * ⚠️ Usar con precaución. Preferir softDelete.
     *
     * @param id Identificador del atributo (Integer)
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura un atributo previamente eliminado.
     *
     * SQL esperado:
     * UPDATE product_attributes SET deleted_at=NULL, is_active=true, updated_at=NOW()
     * WHERE id=$1 AND deleted_at IS NOT NULL
     *
     * @param id Identificador del atributo (Integer)
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminado
     */
    Uni<Boolean> restore(Integer id);
}
