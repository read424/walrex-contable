package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.Currency;
import org.walrex.domain.model.PagedResult;

import java.util.List;
import java.util.Optional;

public interface CurrencyQueryPort {

    /**
     * Busca una moneda activa por su ID.
     *
     * SQL: SELECT * FROM currencies WHERE id = $1 AND deleted_at IS NULL
     *
     * @param id Identificador único
     * @return Uni con Optional de la moneda (vacío si no existe o está eliminada)
     */
    Uni<Optional<Currency>> findById(Integer id);

    /**
     * Busca una moneda por ID incluyendo eliminadas.
     *
     * SQL: SELECT * FROM currencies WHERE id = $1
     *
     * @param id Identificador único
     * @return Uni con Optional de la moneda
     */
    Uni<Optional<Currency>> findByIdIncludingDeleted(Integer id);

    // ==================== Búsquedas por Código ====================

    /**
     * Busca una moneda por código alfabético ISO 4217.
     *
     * SQL: SELECT * FROM currencies WHERE alphabetic_code = $1 AND deleted_at IS NULL
     *
     * @param alphabeticCode Código de 3 letras (ej: USD)
     * @return Uni con Optional de la moneda
     */
    Uni<Optional<Currency>> findByAlphabeticCode(String alphabeticCode);

    /**
     * Busca una moneda por código numérico ISO 4217.
     *
     * SQL: SELECT * FROM currencies WHERE numeric_code = $1 AND deleted_at IS NULL
     *
     * @param numericCode Código de 3 dígitos (ej: 840)
     * @return Uni con Optional de la moneda
     */
    Uni<Optional<Currency>> findByNumericCode(String numericCode);

    /**
     * Busca una moneda por nombre (case-insensitive).
     *
     * SQL: SELECT * FROM currencies WHERE LOWER(name) = LOWER($1) AND deleted_at IS NULL
     *
     * @param name Nombre de la moneda
     * @return Uni con Optional de la moneda
     */
    Uni<Optional<Currency>> findByName(String name);

    // ==================== Verificaciones de Existencia ====================

    /**
     * Verifica si existe una moneda con el código alfabético.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM currencies WHERE alphabetic_code = $1
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param alphabeticCode Código a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByAlphabeticCode(String alphabeticCode, Integer excludeId);

    /**
     * Verifica si existe una moneda con el código numérico.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM currencies WHERE numeric_code = $1
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param numericCode Código a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByNumericCode(Integer numericCode, Integer excludeId);

    /**
     * Verifica si existe una moneda con el nombre.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM currencies WHERE LOWER(name) = LOWER($1)
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    // ==================== Listados con Paginación ====================

    /**
     * Lista monedas con paginación y filtros.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en CurrencyFilter
     * - ORDER BY basado en PageRequest.sortBy
     * - LIMIT $n OFFSET $m para paginación
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros opcionales
     * @return Uni con resultado paginado incluyendo metadata
     */
    Uni<PagedResult<Currency>> findAll(PageRequest pageRequest, CurrencyFilter filter);

    /**
     * Cuenta el total de monedas que cumplen los filtros.
     *
     * SQL: SELECT COUNT(*) FROM currencies WHERE [conditions]
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(CurrencyFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todas las monedas activas como stream.
     *
     * Usa RowSet y lo convierte a Multi para procesamiento reactivo.
     *
     * @return Multi que emite cada moneda
     */
    Multi<Currency> streamAll();

    /**
     * Obtiene monedas como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada moneda que cumple los filtros
     */
    Multi<Currency> streamWithFilter(CurrencyFilter filter);

    // ==================== Consultas Especiales ====================

    /**
     * Lista todas las monedas eliminadas (para posible restauración).
     *
     * SQL: SELECT * FROM currencies WHERE deleted_at IS NOT NULL
     *
     * @return Uni con lista de monedas eliminadas
     */
    Uni<List<Currency>> findAllDeleted();

    /**
     * Busca monedas por múltiples códigos alfabéticos.
     *
     * SQL: SELECT * FROM currencies WHERE alphabetic_code = ANY($1) AND deleted_at IS NULL
     *
     * Nota: Usar Tuple.of((Object) codes.toArray(new String[0])) para pasar el array
     *
     * @param codes Lista de códigos a buscar
     * @return Uni con lista de monedas encontradas
     */
    Uni<List<Currency>> findByAlphabeticCodes(List<String> codes);
}
