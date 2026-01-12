package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeFilter;
import org.walrex.application.port.output.ProductAttributeQueryPort;
import org.walrex.application.port.output.ProductAttributeRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductAttribute;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductAttributeMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductAttributeRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para ProductAttribute.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (ProductAttributeRepositoryPort, ProductAttributeQueryPort)
 * - Traduce entre el modelo de dominio (ProductAttribute) y la capa de persistencia (ProductAttributeEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 *
 * IMPORTANTE: Este adaptador usa Integer como tipo de ID (auto-generado).
 */
@ApplicationScoped
public class ProductAttributePersistenceAdapter implements ProductAttributeRepositoryPort, ProductAttributeQueryPort {

    @Inject
    ProductAttributeRepository repository;

    @Inject
    ProductAttributeMapper mapper;

    // ==================== ProductAttributeRepositoryPort - Operaciones de Escritura ====================

    @Override
    public Uni<ProductAttribute> save(ProductAttribute productAttribute) {
        return repository.persist(mapper.toEntity(productAttribute))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<ProductAttribute> update(ProductAttribute productAttribute) {
        ProductAttributeEntity entity = mapper.toEntity(productAttribute);
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

    // ==================== ProductAttributeQueryPort - Búsquedas por ID ====================

    @Override
    public Uni<Optional<ProductAttribute>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<ProductAttribute>> findByIdIncludingDeleted(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductAttributeQueryPort - Búsquedas por Nombre ====================

    @Override
    public Uni<Optional<ProductAttribute>> findByName(String name) {
        return repository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductAttributeQueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return repository.existsByName(name, excludeId);
    }

    // ==================== ProductAttributeQueryPort - Listados con Paginación ====================

    @Override
    public Uni<PagedResult<ProductAttribute>> findAll(PageRequest pageRequest, ProductAttributeFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<ProductAttribute>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<ProductAttribute>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(ProductAttributeFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== ProductAttributeQueryPort - Listados sin Paginación ====================
    @Override
    public Uni<List<ProductAttribute>> findAllWithFilter(ProductAttributeFilter filter) {
        return repository.findAllWithFilters(filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();
    }

    // ==================== ProductAttributeQueryPort - Streaming ====================

    @Override
    public Multi<ProductAttribute> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<ProductAttribute> streamWithFilter(ProductAttributeFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== ProductAttributeQueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<ProductAttribute>> findAllDeleted() {
        return repository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }
}
