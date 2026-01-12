package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada para eliminar atributos de producto.
 *
 * Siguiendo el patrón hexagonal, este puerto define el contrato
 * para el caso de uso de eliminación de atributos (soft delete).
 */
public interface DeleteProductAttributeUseCase {
    /**
     * Elimina lógicamente un atributo de producto (soft delete).
     *
     * @param id Identificador del atributo a eliminar (Integer)
     * @return Uni<Boolean> true si se eliminó exitosamente
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura un atributo previamente eliminado.
     *
     * @param id Identificador del atributo a restaurar (Integer)
     * @return Uni<Boolean> true si se restauró exitosamente
     */
    Uni<Boolean> restore(Integer id);
}
