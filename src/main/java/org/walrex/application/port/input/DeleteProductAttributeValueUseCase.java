package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

/**
 * Puerto de entrada (Input Port) para eliminar un valor de atributo de producto.
 *
 * Este caso de uso encapsula la lógica de negocio para eliminar valores de atributos:
 * 1. Verificar que el valor de atributo existe
 * 2. Realizar soft delete (setear deletedAt)
 * 3. Invalidar caché
 *
 * Por defecto se realiza eliminación lógica (soft delete).
 * Para eliminación física (hard delete), usar el método hardDelete.
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación e IMPLEMENTADO
 * por el servicio de dominio.
 */
public interface DeleteProductAttributeValueUseCase {

    /**
     * Elimina lógicamente un valor de atributo de producto (soft delete).
     *
     * @param id ID del valor de atributo a eliminar (Integer)
     * @return Uni<Boolean> true si se eliminó correctamente, false si no existía
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura un valor de atributo de producto previamente eliminado.
     *
     * @param id ID del valor de atributo a restaurar (Integer)
     * @return Uni<Boolean> true si se restauró correctamente, false si no existía o no estaba eliminado
     */
    Uni<Boolean> restore(Integer id);
}
