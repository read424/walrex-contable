package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.AccountingAccountResponse;
import org.walrex.application.dto.response.AccountingAccountSelectResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.AccountingAccountCachePort;
import org.walrex.application.port.output.AccountingAccountQueryPort;
import org.walrex.application.port.output.AccountingAccountRepositoryPort;
import org.walrex.domain.exception.AccountingAccountNotFoundException;
import org.walrex.domain.exception.DuplicateAccountingAccountException;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.infrastructure.adapter.inbound.mapper.AccountingAccountDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.AccountingAccountCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.AccountingAccountCache;

import java.time.Duration;
import java.util.List;

/**
 * Servicio de dominio para cuentas contables.
 *
 * Implementa todos los casos de uso relacionados con cuentas siguiendo
 * el patrón hexagonal. Orquesta la lógica de negocio, validaciones,
 * operaciones de persistencia y caché.
 */
@Slf4j
@Transactional
@ApplicationScoped
public class AccountingAccountService implements
        CreateAccountingAccountUseCase,
        ListAccountingAccountsUseCase,
        GetAccountingAccountUseCase,
        UpdateAccountingAccountUseCase,
        DeleteAccountingAccountUseCase {

    @Inject
    AccountingAccountRepositoryPort accountRepositoryPort;

    @Inject
    AccountingAccountQueryPort accountQueryPort;

    @Inject
    @AccountingAccountCache
    AccountingAccountCachePort accountCachePort;

    @Inject
    AccountingAccountDtoMapper accountDtoMapper;

    // TTL del cache: 5 minutos para listado paginado
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // TTL del cache: 15 minutos para listado completo (/all endpoint)
    // Mayor TTL porque los datos son más estáticos y se invalida en cada cambio
    private static final Duration CACHE_ALL_TTL = Duration.ofMinutes(15);

    // ==================== CreateAccountingAccountUseCase ====================

    /**
     * Crea una nueva cuenta contable.
     *
     * @throws DuplicateAccountingAccountException si el código o nombre ya existe
     */
    @Override
    public Uni<AccountingAccount> execute(AccountingAccount accountingAccount) {
        log.info("Creating accounting account: {} ({})", accountingAccount.getName(), accountingAccount.getCode());

        // Validar unicidad de código y nombre
        return validateUniqueness(accountingAccount.getCode(), accountingAccount.getName(), null)
                .onItem().transformToUni(v -> accountRepositoryPort.save(accountingAccount))
                .call(savedAccountingAccount -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating accounting account cache after creation");
                    return accountCachePort.invalidateAll();
                });
    }

    // ==================== ListAccountingAccountsUseCase ====================

    /**
     * Lista cuentas con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, type, etc.)
     * @return Uni con respuesta paginada
     */
    @Override
    public Uni<PagedResponse<AccountingAccountResponse>> execute(PageRequest pageRequest, AccountingAccountFilter filter) {
        log.info("Listing accountingAccounts with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = AccountingAccountCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return accountCachePort.get(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchFromDatabaseAndCache(pageRequest, filter, cacheKey);
                });
    }

    /**
     * Consulta la DB y cachea el resultado.
     */
    private Uni<PagedResponse<AccountingAccountResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            AccountingAccountFilter filter,
            String cacheKey) {

        return accountQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(accountDtoMapper::toResponse)
                            .toList();

                    // Convert page from 0-based (backend) to 1-based (frontend)
                    return PagedResponse.of(
                            responses,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements()
                    );
                })
                .call(result -> {
                    // Cachear el resultado (fire-and-forget)
                    log.debug("Caching result for key: {}", cacheKey);
                    return accountCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todas las cuentas que cumplen el filtro sin paginación.
     * Retorna un DTO optimizado para componentes de selección.
     *
     * Implementa cache-aside pattern con TTL de 15 minutos:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado por 15 minutos
     * 4. Devuelve el resultado
     *
     * El cache se invalida automáticamente en create/update/delete.
     *
     * @param filter Filtros opcionales (por defecto solo cuentas activas)
     * @return Uni con lista completa de cuentas optimizadas
     */
    @Override
    public Uni<List<AccountingAccountSelectResponse>> findAll(AccountingAccountFilter filter) {
        log.info("Listing all accountingAccounts with filter: {}", filter);

        // Generar clave única de cache
        String cacheKey = AccountingAccountCacheKeyGenerator.generateKey(filter);

        // Cache-aside pattern
        return accountCachePort.<AccountingAccountSelectResponse>getList(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchAllAndCache(filter, cacheKey);
                });
    }

    /**
     * Consulta la DB y cachea el resultado para endpoint /all.
     */
    private Uni<List<AccountingAccountSelectResponse>> fetchAllAndCache(
            AccountingAccountFilter filter,
            String cacheKey) {

        return accountQueryPort.findAllWithFilter(filter)
                .onItem().transform(accountDtoMapper::toSelectResponseList)
                .call(result -> {
                    // Cachear el resultado por 15 minutos (fire-and-forget)
                    log.debug("Caching result for key: {} with TTL of {} minutes",
                            cacheKey, CACHE_ALL_TTL.toMinutes());
                    return accountCachePort.putList(cacheKey, result, CACHE_ALL_TTL);
                });
    }

    /**
     * Obtiene todas las cuentas activas como un stream reactivo.
     *
     * @return Multi que emite cada cuenta individualmente
     */
    @Override
    public Multi<AccountingAccountResponse> streamAll() {
        log.info("Streaming all active accountingAccounts");
        return accountQueryPort.streamAll()
                .onItem().transform(accountDtoMapper::toResponse);
    }

    /**
     * Obtiene todas las cuentas activas como un stream con filtros.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada cuenta que cumple los filtros
     */
    @Override
    public Multi<AccountingAccountResponse> streamWithFilter(AccountingAccountFilter filter) {
        log.info("Streaming accountingAccounts with filter: {}", filter);
        return accountQueryPort.streamWithFilter(filter)
                .onItem().transform(accountDtoMapper::toResponse);
    }

    // ==================== GetAccountingAccountUseCase ====================

    /**
     * Obtiene una cuenta por su ID.
     *
     * @throws AccountingAccountNotFoundException si no existe una cuenta con el ID proporcionado
     */
    @Override
    public Uni<AccountingAccount> findById(Integer id) {
        log.info("Getting accountingAccount by id: {}", id);
        return accountQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new AccountingAccountNotFoundException(id)
                ));
    }

    /**
     * Obtiene una cuenta por su código único.
     *
     * @throws AccountingAccountNotFoundException si no existe una cuenta con el código proporcionado
     */
    @Override
    public Uni<AccountingAccount> findByCode(String code) {
        log.info("Getting accountingAccount by code: {}", code);
        return accountQueryPort.findByCode(code)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new AccountingAccountNotFoundException("AccountingAccount not found with code: " + code)
                ));
    }

    // ==================== UpdateAccountingAccountUseCase ====================

    /**
     * Actualiza una cuenta existente con nuevos datos.
     */
    @Override
    public Uni<AccountingAccount> execute(Integer id, AccountingAccount accountingAccount) {
        log.info("Updating accountingAccount id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(accountingAccount.getCode(), accountingAccount.getName(), id)
                .onItem().transformToUni(v -> accountRepositoryPort.update(accountingAccount))
                .call(updatedAccountingAccount -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating accountingAccount cache after update");
                    return accountCachePort.invalidateAll();
                });
    }

    // ==================== DeleteAccountingAccountUseCase ====================

    /**
     * Elimina lógicamente una cuenta (soft delete).
     */
    @Override
    public Uni<Boolean> execute(Integer id) {
        log.info("Soft deleting accountingAccount id: {}", id);
        return accountRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating accountingAccount cache after deletion");
                        return accountCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Restaura una cuenta previamente eliminada.
     */
    @Override
    public Uni<Boolean> restore(Integer id) {
        log.info("Restoring accountingAccount id: {}", id);
        return accountRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating accountingAccount cache after restoration");
                        return accountCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    // ==================== Validaciones ====================

    /**
     * Valida que el código y nombre sean únicos.
     */
    private Uni<Void> validateUniqueness(
            String code,
            String name,
            Integer excludeId) {

        return accountQueryPort.existsByCode(code, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateAccountingAccountException("code", code));
                    }
                    return accountQueryPort.existsByName(name, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateAccountingAccountException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
