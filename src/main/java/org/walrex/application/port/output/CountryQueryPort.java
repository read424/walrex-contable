package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.CountryFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.Country;
import org.walrex.domain.model.PagedResult;

import java.util.List;
import java.util.Optional;

public interface CountryQueryPort {
    /**
     * Busca un pais activa por su ID.
     *
     * SQL: SELECT * FROM country WHERE id = $1 AND deleted_at IS NULL
     *
     * @param id Identificador único
     * @return Uni con Optional del país (vacío si no existe o está eliminada)
     */
    Uni<Optional<Country>> findById(Integer id);

    /**
     * Busca un pais por ID incluyendo eliminadas.
     *
     * SQL: SELECT * FROM country WHERE id = $1
     *
     * @param id Identificador único
     * @return Uni con Optional del pais
     */
    Uni<Optional<Country>> findByIdIncludingDeleted(Integer id);

    // ==================== Búsquedas por Código ====================

    /**
     * Busca un país por código alfabético ISO 3166.
     *
     * SQL: SELECT * FROM country WHERE alphabetic_code = $1 AND deleted_at IS NULL
     *
     * @param alphabeticCode Código de 3 letras (ej: ARG)
     * @return Uni con Optional del pais
     */
    Uni<Optional<Country>> findByAlphabeticCode(String alphabeticCode);

    /**
     * Busca un pais por código numérico ISO 3166.
     *
     * SQL: SELECT * FROM country WHERE numeric_code = $1 AND deleted_at IS NULL
     *
     * @param numericCode Código de 3 dígitos (ej: 004)
     * @return Uni con Optional del pais
     */
    Uni<Optional<Country>> findByNumericCode(String numericCode);

    /**
     * Busca un pais por nombre (case-insensitive).
     *
     * SQL: SELECT * FROM country WHERE LOWER(name) = LOWER($1) AND deleted_at IS NULL
     *
     * @param name Nombre del pais
     * @return Uni con Optional del pais
     */
    Uni<Optional<Country>> findByName(String name);

    /**
     * Verifica si existe un pais con el código alfabético.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM country WHERE alphabetic_code = $1
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param alphabeticCode Código a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByAlphabeticCode(String alphabeticCode, Integer excludeId);

    /**
     * Verifica si existe un pais con el código numérico.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM country WHERE numeric_code = $1
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param numericCode Código a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByNumericCode(Integer numericCode, Integer excludeId);

    /**
     * Verifica si existe un pais con el nombre.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM country WHERE LOWER(name) = LOWER($1)
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    /**
     * Lista paises con paginación y filtros.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en CountryFilter
     * - ORDER BY basado en PageRequest.sortBy
     * - LIMIT $n OFFSET $m para paginación
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros opcionales
     * @return Uni con resultado paginado incluyendo metadata
     */
    Uni<PagedResult<Country>> findAll(PageRequest pageRequest, CountryFilter filter);

    /**
     * Cuenta el total de paises que cumplen los filtros.
     *
     * SQL: SELECT COUNT(*) FROM country WHERE [conditions]
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(CountryFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todas los paises activas como stream.
     *
     * Usa RowSet y lo convierte a Multi para procesamiento reactivo.
     *
     * @return Multi que emite cada pais
     */
    Multi<Country> streamAll();

    /**
     * Obtiene pais como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada pais que cumple los filtros
     */
    Multi<Country> streamWithFilter(CountryFilter filter);

    /**
     * Lista todas los paises eliminadas (para posible restauración).
     *
     * SQL: SELECT * FROM country WHERE deleted_at IS NOT NULL
     *
     * @return Uni con lista de paises eliminadas
     */
    Uni<List<Country>> findAllDeleted();

    /**
     * Busca paises por múltiples códigos alfabéticos.
     *
     * SQL: SELECT * FROM country WHERE alphabetic_code = ANY($1) AND deleted_at IS NULL
     *
     * Nota: Usar Tuple.of((Object) codes.toArray(new String[0])) para pasar el array
     *
     * @param codes Lista de códigos a buscar
     * @return Uni con lista de paises encontradas
     */
    Uni<List<Country>> findByAlphabeticCodes(List<String> codes);

}
