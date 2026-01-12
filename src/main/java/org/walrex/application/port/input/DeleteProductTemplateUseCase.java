package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Caso de uso para eliminar una plantilla de producto.
 */
public interface DeleteProductTemplateUseCase {

    /**
     * Elimina lógicamente una plantilla de producto (soft delete).
     *
     * @param id ID de la plantilla a eliminar
     * @return Uni<Boolean> true si se eliminó correctamente
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura una plantilla de producto previamente eliminada.
     *
     * @param id ID de la plantilla a restaurar
     * @return Uni<Boolean> true si se restauró correctamente
     */
    Uni<Boolean> restore(Integer id);
}
