package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.ProductTemplateFilter;
import org.walrex.application.port.output.ProductTemplateQueryPort;
import org.walrex.application.port.output.ProductTemplateRepositoryPort;
import org.walrex.domain.model.ProductTemplate;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductTemplateEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductTemplateMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductTemplateRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para ProductTemplate.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (ProductTemplateRepositoryPort, ProductTemplateQueryPort)
 * - Traduce entre el modelo de dominio (ProductTemplate) y la capa de persistencia (ProductTemplateEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 * - Usa JOIN FETCH para cargar las entidades relacionadas y evitar N+1 queries
 */
@ApplicationScoped
public class ProductTemplatePersistenceAdapter implements ProductTemplateRepositoryPort, ProductTemplateQueryPort {

    @Inject
    ProductTemplateRepository repository;

    @Inject
    ProductTemplateMapper mapper;

    // ==================== ProductTemplateRepositoryPort - Operaciones de Escritura ====================

    @Override
    public Uni<ProductTemplate> save(ProductTemplate productTemplate) {
        ProductTemplateEntity entity = mapper.toEntity(productTemplate);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return repository.persist(entity)
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<ProductTemplate> update(ProductTemplate productTemplate) {
        ProductTemplateEntity entity = mapper.toEntity(productTemplate);
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
                    entity.setStatus("inactive");
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
                    entity.setStatus("active");
                    entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    return repository.persist(entity)
                            .call(() -> repository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    // ==================== ProductTemplateQueryPort - Búsquedas por ID ====================

    @Override
    public Uni<Optional<ProductTemplate>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<ProductTemplate>> findByInternalReference(String internalReference) {
        return repository.findByInternalReference(internalReference)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== ProductTemplateQueryPort - Búsquedas con Filtros ====================

    @Override
    public Uni<List<ProductTemplate>> findAll() {
        return repository.streamAll()
                .collect().asList()
                .onItem().transform(mapper::toDomainList);
    }

    @Override
    public Uni<List<ProductTemplate>> findAllWithFilter(ProductTemplateFilter filter) {
        return repository.findAllWithFilters(filter)
                .collect().asList()
                .onItem().transform(mapper::toDomainList);
    }

    @Override
    public Uni<Long> count(ProductTemplateFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== ProductTemplateQueryPort - Verificaciones ====================

    @Override
    public Uni<Boolean> existsByInternalReference(String internalReference, Integer excludeId) {
        return repository.existsByInternalReference(internalReference, excludeId);
    }

    @Override
    public Uni<Boolean> existsById(Integer id) {
        return repository.count("id = ?1 and deletedAt is null", id)
                .map(count -> count > 0);
    }
}
