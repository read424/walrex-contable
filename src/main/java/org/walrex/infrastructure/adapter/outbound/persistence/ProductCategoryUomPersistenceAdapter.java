package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductCategoryUomFilter;
import org.walrex.application.port.output.ProductCategoryUomQueryPort;
import org.walrex.application.port.output.ProductCategoryUomRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductCategoryUom;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductCategoryUomEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductCategoryUomMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductCategoryUomRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para ProductCategoryUom.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (ProductCategoryUomRepositoryPort, ProductCategoryUomQueryPort)
 * - Traduce entre el modelo de dominio (ProductCategoryUom) y la capa de persistencia (ProductCategoryUomEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class ProductCategoryUomPersistenceAdapter implements ProductCategoryUomRepositoryPort, ProductCategoryUomQueryPort {

    @Inject
    ProductCategoryUomRepository repository;

    @Inject
    ProductCategoryUomMapper mapper;

    // ==================== ProductCategoryUomRepositoryPort - Operaciones de Escritura ====================

    @Override
    public Uni<ProductCategoryUom> save(ProductCategoryUom productCategoryUom) {
        return repository.persist(mapper.toEntity(productCategoryUom))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<ProductCategoryUom> update(ProductCategoryUom productCategoryUom) {
        ProductCategoryUomEntity entity = mapper.toEntity(productCategoryUom);
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

    // ==================== ProductCategoryUomQueryPort - Búsquedas por ID ====================

    @Override
    public Uni<Optional<ProductCategoryUom>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<ProductCategoryUom>> findByIdIncludingDeleted(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductCategoryUomQueryPort - Búsquedas por Código ====================

    @Override
    public Uni<Optional<ProductCategoryUom>> findByCode(String code) {
        return repository.findByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<ProductCategoryUom>> findByName(String name) {
        return repository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductCategoryUomQueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        return repository.existsByCode(code, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return repository.existsByName(name, excludeId);
    }

    // ==================== ProductCategoryUomQueryPort - Listados con Paginación ====================

    @Override
    public Uni<PagedResult<ProductCategoryUom>> findAll(PageRequest pageRequest, ProductCategoryUomFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<ProductCategoryUom>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<ProductCategoryUom>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(ProductCategoryUomFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== ProductCategoryUomQueryPort - Listados sin Paginación ====================
    @Override
    public Uni<List<ProductCategoryUom>> findAllWithFilter(ProductCategoryUomFilter filter) {
        return repository.findAllWithFilters(filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();
    }

    // ==================== ProductCategoryUomQueryPort - Streaming ====================

    @Override
    public Multi<ProductCategoryUom> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<ProductCategoryUom> streamWithFilter(ProductCategoryUomFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== ProductCategoryUomQueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<ProductCategoryUom>> findAllDeleted() {
        return repository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }
}
