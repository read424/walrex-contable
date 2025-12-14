package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.CurrencyFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.port.output.CurrencyQueryPort;
import org.walrex.application.port.output.CurrencyRepositoryPort;
import org.walrex.domain.model.Currency;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CurrencyEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CurrencyMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CurrencyRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para Currency.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (CurrencyRepositoryPort, CurrencyQueryPort)
 * - Traduce entre el modelo de dominio (Currency) y la capa de persistencia (CurrencyEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class CurrencyPersistenceAdapter implements CurrencyRepositoryPort, CurrencyQueryPort {

    @Inject
    CurrencyRepository repository;

    @Inject
    CurrencyMapper mapper;

    @Override
    public Uni<Currency> save(Currency currency) {

        return repository.persist(mapper.toEntity(currency))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<Currency> update(Currency currency) {
        CurrencyEntity entity = mapper.toEntity(currency);
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
                    entity.setDeletedAt(OffsetDateTime.now());
                    entity.setStatus("1");
                    entity.setUpdatedAt(OffsetDateTime.now());
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
                    entity.setStatus("1");
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return repository.persist(entity)
                            .call(() -> repository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    // ==================== CurrencyQueryPort - Búsquedas por ID ====================

    @Override
    public Uni<Optional<Currency>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<Currency>> findByIdIncludingDeleted(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== CurrencyQueryPort - Búsquedas por Código ====================

    @Override
    public Uni<Optional<Currency>> findByAlphabeticCode(String alphabeticCode) {
        return repository.findByAlphabeticCode(alphabeticCode)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<Currency>> findByNumericCode(String numericCode) {
        return repository.findByNumericCode(numericCode)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<Currency>> findByName(String name) {
        return repository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== CurrencyQueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsByAlphabeticCode(String alphabeticCode, Integer excludeId) {
        return repository.existsByAlphabeticCode(alphabeticCode, excludeId);
    }

    @Override
    public Uni<Boolean> existsByNumericCode(Integer numericCode, Integer excludeId) {
        return repository.existsByNumericCode(numericCode, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return repository.existsByName(name, excludeId);
    }

    @Override
    public Uni<PagedResult<Currency>> findAll(PageRequest pageRequest, CurrencyFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<Currency>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<Currency>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(CurrencyFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== CurrencyQueryPort - Streaming ====================

    @Override
    public Multi<Currency> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<Currency> streamWithFilter(CurrencyFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== CurrencyQueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<Currency>> findAllDeleted() {
        return repository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<List<Currency>> findByAlphabeticCodes(List<String> codes) {
        // Hacemos múltiples búsquedas y las combinamos
        List<Uni<Optional<Currency>>> searches = codes.stream()
                .map(this::findByAlphabeticCode)
                .toList();

        return Uni.combine().all().unis(searches)
                .with(results -> results.stream()
                        .filter(opt -> ((Optional<?>) opt).isPresent())
                        .map(opt -> ((Optional<Currency>) opt).get())
                        .toList());
    }
}
