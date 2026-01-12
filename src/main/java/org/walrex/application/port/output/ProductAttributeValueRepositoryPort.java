package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductAttributeValue;

/**
 * Puerto de salida (Output Port) para operaciones de escritura de valores de atributos de producto.
 *
 * Define las operaciones de persistencia que el dominio necesita:
 * - save: Crear nuevo valor de atributo
 * - update: Actualizar valor de atributo existente
 * - softDelete: Eliminación lógica (setea deletedAt)
 * - hardDelete: Eliminación física (DELETE de DB)
 * - restore: Restaurar valor de atributo eliminado (limpia deletedAt)
 *
 * Patrón Hexagonal:
 * Este puerto es DEFINIDO en la capa de aplicación pero IMPLEMENTADO
 * en la capa de infraestructura (por el Adapter de persistencia).
 *
 * IMPORTANTE: Todos los métodos usan Integer como tipo de ID.
 */
public interface ProductAttributeValueRepositoryPort {

    /**
     * Guarda un nuevo valor de atributo de producto en la base de datos.
     *
     * @param productAttributeValue Valor de atributo a guardar
     * @return Uni con el valor de atributo guardado (incluye timestamps creados)
     */
    Uni<ProductAttributeValue> save(ProductAttributeValue productAttributeValue);

    /**
     * Actualiza un valor de atributo de producto existente.
     *
     * @param productAttributeValue Valor de atributo con los datos actualizados
     * @return Uni con el valor de atributo actualizado (incluye updatedAt actualizado)
     */
    Uni<ProductAttributeValue> update(ProductAttributeValue productAttributeValue);

    /**
     * Realiza una eliminación lógica del valor de atributo (soft delete).
     * Setea el campo deletedAt con la fecha/hora actual.
     *
     * @param id ID del valor de atributo a eliminar (Integer)
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Realiza una eliminación física del valor de atributo (hard delete).
     * ADVERTENCIA: Esta operación es irreversible.
     *
     * @param id ID del valor de atributo a eliminar permanentemente (Integer)
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura un valor de atributo previamente eliminado (limpia deletedAt).
     *
     * @param id ID del valor de atributo a restaurar (Integer)
     * @return Uni<Boolean> true si se restauró, false si no existía o no estaba eliminado
     */
    Uni<Boolean> restore(Integer id);
}
