package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SunatDocumentTypeFilter;
import org.walrex.application.port.output.SunatDocumentTypeQueryPort;
import org.walrex.application.port.output.SunatDocumentTypeRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.SunatDocumentType;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.SunatDocumentTypeEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.SunatDocumentTypeMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.SunatDocumentTypeRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para SunatDocumentType.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (RepositoryPort, QueryPort)
 * - Traduce entre el modelo de dominio (SunatDocumentType) y la capa de persistencia (SunatDocumentTypeEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class SunatDocumentTypePersistenceAdapter implements SunatDocumentTypeRepositoryPort, SunatDocumentTypeQueryPort {

    @Inject
    SunatDocumentTypeRepository repository;

    @Inject
    SunatDocumentTypeMapper mapper;

    // ==================== RepositoryPort - Operaciones de Escritura ====================

    @Override
    public Uni<SunatDocumentType> save(SunatDocumentType documentType) {
        return repository.persist(mapper.toEntity(documentType))
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Uni<SunatDocumentType> update(SunatDocumentType documentType) {
        return repository.findById(documentType.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("SunatDocumentType not found with id: " + documentType.getId())
                        );
                    }

                    // Actualizar solo los campos modificables
                    existingEntity.setCode(documentType.getCode());
                    existingEntity.setName(documentType.getName());
                    existingEntity.setDescription(documentType.getDescription());
                    existingEntity.setLength(documentType.getLength());
                    existingEntity.setPattern(documentType.getPattern());
                    existingEntity.setActive(documentType.getActive());
                    existingEntity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                    // persist() actualizará la entidad existente porque ya está managed
                    return repository.persist(existingEntity)
                            .onItem().transform(mapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> deactivate(Integer id) {
        return repository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || !entity.getActive()) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setActive(false);
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return repository.persist(entity)
                            .call(() -> repository.flush())  // Forzar flush para asegurar que se escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> activate(Integer id) {
        return repository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getActive()) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setActive(true);
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

    // ==================== QueryPort - Operaciones de Lectura ====================

    @Override
    public Uni<Optional<SunatDocumentType>> findById(Integer id) {
        return repository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<SunatDocumentType>> findByIdIncludingInactive(Integer id) {
        return repository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<Optional<SunatDocumentType>> findByCode(String code) {
        return repository.findByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    // ==================== QueryPort - Verificaciones de Existencia ====================

    @Override
    public Uni<Boolean> existsById(Integer id, String excludeId) {
        return repository.existsById(id, excludeId);
    }

    @Override
    public Uni<Boolean> existsByCode(String code, String excludeId) {
        return repository.existsByCode(code, excludeId);
    }

    // ==================== QueryPort - Listados con Paginación ====================

    @Override
    public Uni<PagedResult<SunatDocumentType>> findAll(PageRequest pageRequest, SunatDocumentTypeFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<SunatDocumentType>> dataUni = repository.findWithFilters(pageRequest, filter)
                .onItem().transform(mapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = repository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),              // List<SunatDocumentType>
                        pageRequest.getPage(),         // page
                        pageRequest.getSize(),         // size
                        tuple.getItem2()               // totalElements
                ));
    }

    @Override
    public Uni<Long> count(SunatDocumentTypeFilter filter) {
        return repository.countWithFilters(filter);
    }

    // ==================== QueryPort - Streaming ====================

    @Override
    public Multi<SunatDocumentType> streamAll() {
        return repository.streamAll()
                .onItem().transform(mapper::toDomain);
    }

    @Override
    public Multi<SunatDocumentType> streamWithFilter(SunatDocumentTypeFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== QueryPort - Consultas Especiales ====================

    @Override
    public Uni<List<SunatDocumentType>> findAllInactive() {
        return repository.findAllInactive()
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<List<SunatDocumentType>> findByLength(Integer length) {
        return repository.findByLength(length)
                .onItem().transform(entities -> entities.stream()
                        .map(mapper::toDomain)
                        .toList());
    }
}
