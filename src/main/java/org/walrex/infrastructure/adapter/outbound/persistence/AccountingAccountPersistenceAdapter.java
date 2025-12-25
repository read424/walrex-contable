package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.AccountingAccountFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.port.output.AccountingAccountQueryPort;
import org.walrex.application.port.output.AccountingAccountRepositoryPort;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.AccountingAccountMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.AccountingAccountRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para AccountingAccount.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (AccountingAccountRepositoryPort, AccountingAccountQueryPort)
 * - Traduce entre el modelo de dominio (AccountingAccount) y la capa de persistencia (AccountingAccountEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class AccountingAccountPersistenceAdapter implements AccountingAccountRepositoryPort, AccountingAccountQueryPort {

    @Inject
    AccountingAccountRepository repository;

    @Inject
    AccountingAccountMapper mapper;

    // ==================== AccountingAccountRepositoryPort - Operaciones de Escritura ====================

    @Override
    public Uni<AccountingAccount> save(AccountingAccount accountingAccountingAccount) {
        return repository.persist(mapper.toEntity(accountingAccountingAccount))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<AccountingAccount> update(AccountingAccount accountingAccountingAccount) {
        AccountingAccountEntity entity = mapper.toEntity(accountingAccountingAccount);
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.persist(entity)
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        return repository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() != null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    entity.setActive(false);
                    entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    return repository.persist(entity)
                            .call(() -> repository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> hardDelete(Integer id) {
        return repository.deleteById(id);
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        return repository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() == null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(null);
                    entity.setActive(true);
                    entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    return repository.persist(entity)
                            .call(() -> repository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    // ==================== AccountingAccountQueryPort - Búsquedas por ID ====================

    @Override
    public Uni<Optional<AccountingAccount>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<AccountingAccount>> findByIdIncludingDeleted(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== AccountingAccountQueryPort - Búsquedas por Código ====================

    @Override
    public Uni<Optional<AccountingAccount>> findByCode(String code) {
        return repository.findByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<AccountingAccount>> findByName(String name) {
        return repository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== AccountingAccountQueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        return repository.existsByCode(code, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return repository.existsByName(name, excludeId);
    }

    // ==================== AccountingAccountQueryPort - Listados con Paginación ====================

    @Override
    public Uni<PagedResult<AccountingAccount>> findAll(PageRequest pageRequest, AccountingAccountFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<AccountingAccount>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<AccountingAccount>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(AccountingAccountFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== AccountingAccountQueryPort - Listados sin Paginación ====================
    @Override
    public Uni<List<AccountingAccount>> findAllWithFilter(AccountingAccountFilter filter) {
        return repository.findAllWithFilters(filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();
    }

    // ==================== AccountingAccountQueryPort - Streaming ====================

    @Override
    public Multi<AccountingAccount> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<AccountingAccount> streamWithFilter(AccountingAccountFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== AccountingAccountQueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<AccountingAccount>> findAllDeleted() {
        return repository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }
}
