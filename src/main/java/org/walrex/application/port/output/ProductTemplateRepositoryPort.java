package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductTemplate;

/**
 * Puerto de salida para operaciones de escritura de plantillas de producto.
 *
 * Define el contrato para operaciones de persistencia (CREATE, UPDATE, DELETE).
 * Será implementado por el adaptador de persistencia.
 */
public interface ProductTemplateRepositoryPort {

    /**
     * Guarda una nueva plantilla de producto.
     *
     * @param productTemplate Plantilla de producto a guardar
     * @return Uni con la plantilla guardada (incluyendo ID generado)
     */
    Uni<ProductTemplate> save(ProductTemplate productTemplate);

    /**
     * Actualiza una plantilla de producto existente.
     *
     * @param productTemplate Plantilla de producto con datos actualizados
     * @return Uni con la plantilla actualizada
     */
    Uni<ProductTemplate> update(ProductTemplate productTemplate);

    /**
     * Elimina lógicamente una plantilla de producto (soft delete).
     *
     * @param id ID de la plantilla a eliminar
     * @return Uni<Boolean> true si se eliminó correctamente
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente una plantilla de producto (hard delete).
     *
     * @param id ID de la plantilla a eliminar permanentemente
     * @return Uni<Boolean> true si se eliminó correctamente
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura una plantilla de producto previamente eliminada.
     *
     * @param id ID de la plantilla a restaurar
     * @return Uni<Boolean> true si se restauró correctamente
     */
    Uni<Boolean> restore(Integer id);
}
