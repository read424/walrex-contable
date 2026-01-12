package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductUomFilter;
import org.walrex.application.port.output.ProductUomQueryPort;
import org.walrex.application.port.output.ProductUomRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductUom;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductUomEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductUomMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductUomRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para ProductUom.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (ProductUomRepositoryPort, ProductUomQueryPort)
 * - Traduce entre el modelo de dominio (ProductUom) y la capa de persistencia (ProductUomEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 * - Usa JOIN FETCH para cargar la categoría relacionada y evitar N+1 queries
 */
@ApplicationScoped
public class ProductUomPersistenceAdapter implements ProductUomRepositoryPort, ProductUomQueryPort {

    @Inject
    ProductUomRepository repository;

    @Inject
    ProductUomMapper mapper;

    // ==================== ProductUomRepositoryPort - Operaciones de Escritura ====================

    @Override
    public Uni<ProductUom> save(ProductUom productUom) {
        return repository.persist(mapper.toEntity(productUom))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<ProductUom> update(ProductUom productUom) {
        ProductUomEntity entity = mapper.toEntity(productUom);
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

    // ==================== ProductUomQueryPort - Búsquedas por ID ====================

    @Override
    public Uni<Optional<ProductUom>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<ProductUom>> findByIdIncludingDeleted(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductUomQueryPort - Búsquedas por Código ====================

    @Override
    public Uni<Optional<ProductUom>> findByCode(String code) {
        return repository.findByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<ProductUom>> findByName(String name) {
        return repository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductUomQueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        return repository.existsByCode(code, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return repository.existsByName(name, excludeId);
    }

    // ==================== ProductUomQueryPort - Listados con Paginación ====================

    @Override
    public Uni<PagedResult<ProductUom>> findAll(PageRequest pageRequest, ProductUomFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<ProductUom>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<ProductUom>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(ProductUomFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== ProductUomQueryPort - Listados sin Paginación ====================
    @Override
    public Uni<List<ProductUom>> findAllWithFilter(ProductUomFilter filter) {
        return repository.findAllWithFilters(filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();
    }

    // ==================== ProductUomQueryPort - Streaming ====================

    @Override
    public Multi<ProductUom> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<ProductUom> streamWithFilter(ProductUomFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== ProductUomQueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<ProductUom>> findAllDeleted() {
        return repository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }
}
