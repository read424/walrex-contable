package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.PagedResult;

import java.util.Optional;

public interface DepartamentQueryPort {

    /**
     * Busca un departamento activo por su ID.
     *
     * @param id Identificador único
     * @return Uni con Optional del departamento
     */
    Uni<Optional<Departament>> findById(Integer id);

    /**
     * Busca un departamento por ID incluyendo eliminados.
     *
     * @param id Identificador único
     * @return Uni con Optional del departamento
     */
    Uni<Optional<Departament>> findByIdIncludingDeleted(Integer id);

    /**
     * Busca un departamento por código.
     *
     * @param code Código del departamento
     * @return Uni con Optional del departamento
     */
    Uni<Optional<Departament>> findByCode(String code);

    /**
     * Busca un departamento por nombre.
     *
     * @param name Nombre del departamento
     * @return Uni con Optional del departamento
     */
    Uni<Optional<Departament>> findByName(String name);

    /**
     * Verifica si existe un departamento con el código.
     *
     * @param code      Código a verificar
     * @param excludeId ID a excluir (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByCode(String code, Integer excludeId);

    /**
     * Verifica si existe un departamento con el nombre.
     *
     * @param name      Nombre a verificar
     * @param excludeId ID a excluir (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    /**
     * Lista departamentos con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación
     * @param filter      Filtros opcionales
     * @return Uni con resultado paginado
     */
    Uni<PagedResult<Departament>> findAll(PageRequest pageRequest, DepartamentFilter filter);

    /**
     * Cuenta el total de departamentos que cumplen los filtros.
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(DepartamentFilter filter);

    /**
     * Obtiene todos los departamentos activos como stream.
     *
     * @return Multi que emite cada departamento
     */
    Multi<Departament> streamAll();

    /**
     * Obtiene departamentos como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada departamento filtrado
     */
    Multi<Departament> streamWithFilter(DepartamentFilter filter);
}
