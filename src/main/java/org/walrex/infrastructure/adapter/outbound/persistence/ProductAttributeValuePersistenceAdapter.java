package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProductAttributeValueFilter;
import org.walrex.application.port.output.ProductAttributeValueQueryPort;
import org.walrex.application.port.output.ProductAttributeValueRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.ProductAttributeValue;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductAttributeValueEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductAttributeValueMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductAttributeValueRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia para ProductAttributeValue.
 *
 * Implementa los puertos de salida (output ports) definidos en la capa de aplicación:
 * - ProductAttributeValueRepositoryPort: Operaciones de escritura (save, update, delete)
 * - ProductAttributeValueQueryPort: Operaciones de lectura (find, list, count)
 *
 * Este adaptador es la implementación concreta del patrón hexagonal en la capa de infraestructura.
 * Traduce entre el modelo de dominio (ProductAttributeValue) y la entidad de persistencia (ProductAttributeValueEntity).
 *
 * Usa:
 * - ProductAttributeValueRepository: Para operaciones de base de datos con Panache
 * - ProductAttributeValueMapper: Para convertir entre entidad y dominio
 *
 * IMPORTANTE: Todos los métodos manejan Integer como tipo de ID.
 */
@Slf4j
@ApplicationScoped
public class ProductAttributeValuePersistenceAdapter implements
        ProductAttributeValueRepositoryPort,
        ProductAttributeValueQueryPort {

    @Inject
    ProductAttributeValueRepository repository;

    @Inject
    ProductAttributeValueMapper mapper;

    // ==================== ProductAttributeValueRepositoryPort Implementation ====================

    @Override
    @WithTransaction
    public Uni<ProductAttributeValue> save(ProductAttributeValue productAttributeValue) {
        log.debug("Saving product attribute value: {}", productAttributeValue.getId());

        // Setear timestamps
        productAttributeValue.setCreatedAt(OffsetDateTime.now());
        productAttributeValue.setUpdatedAt(OffsetDateTime.now());

        // Convertir a entidad y persistir
        ProductAttributeValueEntity entity = mapper.toEntity(productAttributeValue);
        return repository.persist(entity)
                .onItem().transform(mapper::toDomain);
    }

    @Override
    @WithTransaction
    public Uni<ProductAttributeValue> update(ProductAttributeValue productAttributeValue) {
        log.debug("Updating product attribute value: {}", productAttributeValue.getId());

        // Actualizar timestamp
        productAttributeValue.setUpdatedAt(OffsetDateTime.now());

        // Buscar entidad existente
        return repository.findById(productAttributeValue.getId())
                .onItem().ifNotNull().transformToUni(entity -> {
                    // Actualizar campos
                    entity.setName(productAttributeValue.getName());
                    entity.setHtmlColor(productAttributeValue.getHtmlColor());
                    entity.setSequence(productAttributeValue.getSequence());
                    entity.setActive(productAttributeValue.getActive());
                    entity.setUpdatedAt(productAttributeValue.getUpdatedAt());

                    return repository.persist(entity)
                            .onItem().transform(mapper::toDomain);
                })
                .onItem().ifNull().continueWith(() -> productAttributeValue);
    }

    @Override
    @WithTransaction
    public Uni<Boolean> softDelete(Integer id) {
        log.debug("Soft deleting product attribute value: {}", id);

        return repository.findById(id)
                .onItem().ifNotNull().transformToUni(entity -> {
                    entity.setDeletedAt(OffsetDateTime.now());
                    return repository.persist(entity)
                            .replaceWith(true);
                })
                .onItem().ifNull().continueWith(false);
    }

    @Override
    @WithTransaction
    public Uni<Boolean> hardDelete(Integer id) {
        log.debug("Hard deleting product attribute value: {}", id);

        return repository.findById(id)
                .onItem().ifNotNull().transformToUni(entity ->
                        repository.delete(entity).replaceWith(true)
                )
                .onItem().ifNull().continueWith(false);
    }

    @Override
    @WithTransaction
    public Uni<Boolean> restore(Integer id) {
        log.debug("Restoring product attribute value: {}", id);

        return repository.findById(id)
                .onItem().ifNotNull().transformToUni(entity -> {
                    if (entity.getDeletedAt() != null) {
                        entity.setDeletedAt(null);
                        entity.setUpdatedAt(OffsetDateTime.now());
                        return repository.persist(entity)
                                .replaceWith(true);
                    }
                    return Uni.createFrom().item(false);
                })
                .onItem().ifNull().continueWith(false);
    }

    // ==================== ProductAttributeValueQueryPort Implementation ====================

    @Override
    public Uni<Optional<ProductAttributeValue>> findById(Integer id) {
        log.debug("Finding product attribute value by id: {}", id);

        return repository.findActiveById(id)
                .onItem().ifNotNull().transform(entity -> Optional.of(mapper.toDomain(entity)))
                .onItem().ifNull().continueWith(Optional.empty());
    }

    @Override
    public Uni<PagedResult<ProductAttributeValue>> findAll(PageRequest pageRequest, ProductAttributeValueFilter filter) {
        log.debug("Finding all product attribute values with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Obtener datos paginados
        Uni<List<ProductAttributeValue>> contentUni = repository.findWithFilters(pageRequest, filter)
                .collect().asList()
                .onItem().transform(mapper::toDomainList);

        // Obtener total de elementos
        Uni<Long> totalUni = repository.countWithFilters(filter);

        // Combinar resultados
        return Uni.combine().all().unis(contentUni, totalUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),
                        pageRequest.getPage(),
                        pageRequest.getSize(),
                        tuple.getItem2()
                ));
    }

    @Override
    public Uni<List<ProductAttributeValue>> findAllWithFilter(ProductAttributeValueFilter filter) {
        log.debug("Finding all product attribute values with filter: {}", filter);

        return repository.findAllWithFilters(filter)
                .collect().asList()
                .onItem().transform(mapper::toDomainList);
    }

    @Override
    public Uni<Long> count(ProductAttributeValueFilter filter) {
        log.debug("Counting product attribute values with filter: {}", filter);
        return repository.countWithFilters(filter);
    }

    @Override
    public Uni<Optional<ProductAttributeValue>> findByName(String name) {
        log.debug("Finding product attribute value by name: {}", name);

        return repository.findByName(name)
                .onItem().ifNotNull().transform(entity -> Optional.of(mapper.toDomain(entity)))
                .onItem().ifNull().continueWith(Optional.empty());
    }

    @Override
    public Uni<List<ProductAttributeValue>> findByAttributeId(Integer attributeId) {
        log.debug("Finding product attribute values by attributeId: {}", attributeId);

        return repository.findByAttributeId(attributeId)
                .onItem().transform(mapper::toDomainList);
    }

    @Override
    public Uni<Boolean> existsById(Integer id, Integer excludeId) {
        log.debug("Checking if product attribute value exists with id: {}, excludeId: {}", id, excludeId);
        return repository.existsById(id, excludeId);
    }

    @Override
    public Uni<Boolean> existsByAttributeIdAndName(Integer attributeId, String name, Integer excludeId) {
        log.debug("Checking if product attribute value exists with attributeId: {}, name: {}, excludeId: {}",
                attributeId, name, excludeId);
        return repository.existsByAttributeIdAndName(attributeId, name, excludeId);
    }

    @Override
    public Multi<ProductAttributeValue> streamAll() {
        log.debug("Streaming all active product attribute values");

        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<ProductAttributeValue> streamWithFilter(ProductAttributeValueFilter filter) {
        log.debug("Streaming product attribute values with filter: {}", filter);

        return repository.findAllWithFilters(filter)
                .onItem().transform(mapper::toDomain);
    }
}
