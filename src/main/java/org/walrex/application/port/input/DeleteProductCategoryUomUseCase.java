package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada para eliminar y restaurar categorías de unidades de medida.
 */
public interface DeleteProductCategoryUomUseCase {

    /**
     * Elimina lógicamente una categoría (soft delete).
     *
     * La categoría no se borra físicamente, se marca como eliminada
     * estableciendo la fecha de eliminación.
     *
     * @param id Identificador de la categoría a eliminar
     * @return Uni con true si se eliminó correctamente, false si no se encontró
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si no existe una categoría activa con el ID proporcionado
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura una categoría previamente eliminada.
     *
     * @param id Identificador de la categoría a restaurar
     * @return Uni con true si se restauró correctamente, false si no se encontró
     * @throws org.walrex.domain.exception.ProductCategoryUomNotFoundException
     *         si no existe una categoría eliminada con el ID proporcionado
     */
    Uni<Boolean> restore(Integer id);
}
