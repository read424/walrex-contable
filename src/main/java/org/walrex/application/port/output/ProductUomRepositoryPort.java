package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductUom;

/**
 * Puerto de salida para operaciones de escritura de ProductUom.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de persistencia sin especificar la implementación.
 */
public interface ProductUomRepositoryPort {

    /**
     * Guarda una nueva unidad de medida.
     *
     * @param productUom Unidad de medida a guardar
     * @return Uni con la unidad guardada (incluye ID generado)
     */
    Uni<ProductUom> save(ProductUom productUom);

    /**
     * Actualiza una unidad de medida existente.
     *
     * @param productUom Unidad de medida con datos actualizados
     * @return Uni con la unidad actualizada
     */
    Uni<ProductUom> update(ProductUom productUom);

    /**
     * Elimina lógicamente una unidad de medida (soft delete).
     * Establece deletedAt = now() y active = false.
     *
     * @param id ID de la unidad a eliminar
     * @return Uni<Boolean> true si se eliminó, false si no existía o ya estaba eliminada
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente una unidad de medida de la base de datos.
     * Usar con precaución.
     *
     * @param id ID de la unidad a eliminar
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura una unidad de medida previamente eliminada.
     * Establece deletedAt = null y active = true.
     *
     * @param id ID de la unidad a restaurar
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminada
     */
    Uni<Boolean> restore(Integer id);
}
