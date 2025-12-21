package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.Province;

import java.util.Optional;

public interface ProvinceQueryPort {

    /**
     * Busca una provincia act0ivo por su ID.
     *
     * @param id Identificador único
     * @return Uni con Optional de la provincia
     */
    Uni<Optional<Province>> findById(Integer id);

    /**
     * Busca una provincia por ID incluyendo eliminados.
     *
     * @param id Identificador único
     * @return Uni con Optional de la provincia
     */
    Uni<Optional<Province>> findByIdIncludingDeleted(Integer id);

    /**
     * Busca una provincia por código.
     *
     * @param code Código de la provincia
     * @return Uni con Optional de la provincia
     */
    Uni<Optional<Province>> findByCode(String code);

    /**
     * Busca una provincia por nombre.
     *
     * @param name Nombre de la provincia
     * @return Uni con Optional de la provincia
     */
    Uni<Optional<Province>> findByName(String name);

    /**
     * Verifica si existe una provincia con el código.
     *
     * @param code      Código a verificar
     * @param excludeId ID a excluir (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByCode(String code, Integer excludeId);

    /**
     * Verifica si existe una provincia con el nombre para un mismo departamento.
     *
     * @param name      Nombre a verificar
     * @param excludeId ID a excluir (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByNameForDepartment(String name, Integer idDepartment, Integer excludeId);

    /**
     * Lista provincias con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación
     * @param filter      Filtros opcionales
     * @return Uni con resultado paginado
     */
    Uni<PagedResult<Province>> findAll(PageRequest pageRequest, ProvinceFilter filter);

    /**
     * Cuenta el total de provincias que cumplen los filtros.
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(ProvinceFilter filter);


    /**
     * Cuenta el total de provincias por departamento x.
     *
     * @param idDepartment Filtro id departamento
     * @return Uni con el conteo total
     */
    Uni<Long> countByDepartment(Integer idDepartment);


    /**
     * Obtiene todos las provincias activos por departamento como stream.
     *
     * @return Multi que emite cada provincia
     */
    Multi<Province> findByDepartment(Integer idDepartment);

    /**
     * Obtiene todos las provincias activos como stream.
     *
     * @return Multi que emite cada provincia
     */
    Multi<Province> streamAll();

    /**
     * Obtiene provincias como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada provincia filtrado
     */
    Multi<Province> streamWithFilter(ProvinceFilter filter);

}
