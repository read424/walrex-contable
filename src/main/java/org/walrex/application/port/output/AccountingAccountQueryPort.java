package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.domain.model.PagedResult;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para consultas de cuentas contables.
 *
 * Siguiendo el patrón hexagonal, este puerto define las operaciones
 * de lectura sin especificar la tecnología (PostgreSQL, etc.).
 */
public interface AccountingAccountQueryPort {

    // ==================== Búsquedas por ID ====================

    /**
     * Busca una cuenta activa por su ID.
     *
     * SQL: SELECT * FROM accountingAccounts WHERE id = $1 AND deleted_at IS NULL
     *
     * @param id Identificador único
     * @return Uni con Optional de la cuenta (vacío si no existe o está eliminada)
     */
    Uni<Optional<AccountingAccount>> findById(Integer id);

    /**
     * Busca una cuenta por ID incluyendo eliminadas.
     *
     * SQL: SELECT * FROM accountingAccounts WHERE id = $1
     *
     * @param id Identificador único
     * @return Uni con Optional de la cuenta
     */
    Uni<Optional<AccountingAccount>> findByIdIncludingDeleted(Integer id);

    // ==================== Búsquedas por Código ====================

    /**
     * Busca una cuenta por código único.
     *
     * SQL: SELECT * FROM accountingAccounts WHERE code = $1 AND deleted_at IS NULL
     *
     * @param code Código único de la cuenta
     * @return Uni con Optional de la cuenta
     */
    Uni<Optional<AccountingAccount>> findByCode(String code);

    /**
     * Busca una cuenta por nombre (case-insensitive).
     *
     * SQL: SELECT * FROM accountingAccounts WHERE LOWER(name) = LOWER($1) AND deleted_at IS NULL
     *
     * @param name Nombre de la cuenta
     * @return Uni con Optional de la cuenta
     */
    Uni<Optional<AccountingAccount>> findByName(String name);

    // ==================== Verificaciones de Existencia ====================

    /**
     * Verifica si existe una cuenta con el código.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM accountingAccounts WHERE code = $1
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param code Código a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByCode(String code, Integer excludeId);

    /**
     * Verifica si existe una cuenta con el nombre.
     *
     * SQL: SELECT EXISTS(SELECT 1 FROM accountingAccounts WHERE LOWER(name) = LOWER($1)
     *      AND deleted_at IS NULL [AND id != $2])
     *
     * @param name Nombre a verificar
     * @param excludeId ID a excluir de la búsqueda (null para no excluir)
     * @return Uni<Boolean> true si existe
     */
    Uni<Boolean> existsByName(String name, Integer excludeId);

    // ==================== Listados con Paginación ====================

    /**
     * Lista cuentas con paginación y filtros.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en AccountingAccountFilter
     * - ORDER BY basado en PageRequest.sortBy
     * - LIMIT $n OFFSET $m para paginación
     *
     * @param pageRequest Configuración de paginación
     * @param filter Filtros opcionales
     * @return Uni con resultado paginado incluyendo metadata
     */
    Uni<PagedResult<AccountingAccount>> findAll(PageRequest pageRequest, AccountingAccountFilter filter);

    /**
     * Cuenta el total de cuentas que cumplen los filtros.
     *
     * SQL: SELECT COUNT(*) FROM accountingAccounts WHERE [conditions]
     *
     * @param filter Filtros opcionales
     * @return Uni con el conteo total
     */
    Uni<Long> count(AccountingAccountFilter filter);

    // ==================== Listados sin Paginación ====================

    /**
     * Lista todas las cuentas que cumplen el filtro sin paginación.
     *
     * SQL dinámico con:
     * - WHERE conditions basadas en AccountingAccountFilter
     * - ORDER BY name ASC (por defecto)
     *
     * @param filter Filtros opcionales
     * @return Uni con lista completa de cuentas
     */
    Uni<List<AccountingAccount>> findAllWithFilter(AccountingAccountFilter filter);

    // ==================== Streaming ====================

    /**
     * Obtiene todas las cuentas activas como stream.
     *
     * Usa RowSet y lo convierte a Multi para procesamiento reactivo.
     *
     * @return Multi que emite cada cuenta
     */
    Multi<AccountingAccount> streamAll();

    /**
     * Obtiene cuentas como stream aplicando filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada cuenta que cumple los filtros
     */
    Multi<AccountingAccount> streamWithFilter(AccountingAccountFilter filter);

    // ==================== Consultas Especiales ====================

    /**
     * Lista todas las cuentas eliminadas (para posible restauración).
     *
     * SQL: SELECT * FROM accountingAccounts WHERE deleted_at IS NOT NULL
     *
     * @return Uni con lista de cuentas eliminadas
     */
    Uni<List<AccountingAccount>> findAllDeleted();
}
