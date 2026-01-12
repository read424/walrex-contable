package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada para eliminar unidades de medida de productos.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de eliminación (soft delete) de unidades de medida.
 */
public interface DeleteProductUomUseCase {
    /**
     * Elimina lógicamente una unidad de medida de producto (soft delete).
     *
     * @param id ID de la unidad de medida a eliminar
     * @return Uni<Boolean> true si se eliminó exitosamente, false si no se encontró
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura una unidad de medida previamente eliminada.
     *
     * @param id ID de la unidad de medida a restaurar
     * @return Uni<Boolean> true si se restauró exitosamente, false si no se encontró o no estaba eliminada
     */
    Uni<Boolean> restore(Integer id);
}
