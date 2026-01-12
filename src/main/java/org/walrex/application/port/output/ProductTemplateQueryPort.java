package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.domain.model.ProductTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de consulta de plantillas de producto.
 *
 * Define el contrato para operaciones de lectura (READ, QUERY).
 * Será implementado por el adaptador de persistencia.
 */
public interface ProductTemplateQueryPort {

    /**
     * Busca una plantilla de producto por ID.
     *
     * @param id ID de la plantilla
     * @return Uni con Optional de ProductTemplate (vacío si no existe o está eliminado)
     */
    Uni<Optional<ProductTemplate>> findById(Integer id);

    /**
     * Busca todas las plantillas de producto activas (sin paginación).
     *
     * @return Uni con lista de todas las plantillas activas
     */
    Uni<List<ProductTemplate>> findAll();

    /**
     * Busca todas las plantillas de producto que cumplan los filtros.
     *
     * @param filter Filtros a aplicar
     * @return Uni con lista de plantillas que cumplen los filtros
     */
    Uni<List<ProductTemplate>> findAllWithFilter(ProductTemplateFilter filter);

    /**
     * Cuenta el total de plantillas que cumplen los filtros.
     *
     * @param filter Filtros a aplicar
     * @return Uni con el total de registros
     */
    Uni<Long> count(ProductTemplateFilter filter);

    /**
     * Busca una plantilla de producto por referencia interna.
     *
     * @param internalReference Referencia interna única
     * @return Uni con Optional de ProductTemplate
     */
    Uni<Optional<ProductTemplate>> findByInternalReference(String internalReference);

    /**
     * Verifica si existe una plantilla con la referencia interna dada.
     *
     * @param internalReference Referencia interna a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByInternalReference(String internalReference, Integer excludeId);

    /**
     * Verifica si existe una plantilla con el ID dado.
     *
     * @param id ID a verificar
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsById(Integer id);
}
