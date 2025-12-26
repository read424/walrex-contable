package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.output.SystemDocumentTypeQueryPort;
import org.walrex.application.port.output.SystemDocumentTypeRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.SystemDocumentType;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.SystemDocumentTypeMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.SystemDocumentTypeRepository;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia que implementa los puertos de salida para
 * SystemDocumentType.
 *
 * Siguiendo el patrón hexagonal (Ports & Adapters), este adaptador:
 * - Implementa las interfaces de puerto (SystemDocumentTypeRepositoryPort,
 * SystemDocumentTypeQueryPort)
 * - Traduce entre el modelo de dominio (SystemDocumentType) y la capa de
 * persistencia (SystemDocumentTypeEntity)
 * - Utiliza el mapper para transformaciones
 * - Delega operaciones de persistencia al repository de Panache
 */
@ApplicationScoped
public class SystemDocumentTypePersistenceAdapter
        implements SystemDocumentTypeRepositoryPort, SystemDocumentTypeQueryPort {

    @Inject
    SystemDocumentTypeRepository systemDocumentTypeRepository;

    @Inject
    SystemDocumentTypeMapper systemDocumentTypeMapper;

    // ==================== SystemDocumentTypeRepositoryPort - Operaciones de
    // Escritura ====================

    @Override
    public Uni<SystemDocumentType> save(SystemDocumentType systemDocumentType) {
        return systemDocumentTypeRepository.persist(systemDocumentTypeMapper.toEntity(systemDocumentType))
                .onItem().transform(systemDocumentTypeMapper::toDomain);
    }

    @Override
    public Uni<SystemDocumentType> update(SystemDocumentType systemDocumentType) {
        return systemDocumentTypeRepository.findById(systemDocumentType.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException(
                                        "System document type not found with id: " + systemDocumentType.getId()));
                    }

                    // Actualizar solo los campos modificables
                    existingEntity.setCode(systemDocumentType.getCode());
                    existingEntity.setName(systemDocumentType.getName());
                    existingEntity.setDescription(systemDocumentType.getDescription());
                    existingEntity.setIsRequired(systemDocumentType.getIsRequired());
                    existingEntity.setForPerson(systemDocumentType.getForPerson());
                    existingEntity.setForCompany(systemDocumentType.getForCompany());
                    existingEntity.setPriority(systemDocumentType.getPriority());
                    existingEntity.setActive(systemDocumentType.getActive());
                    existingEntity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                    // persist() actualizará la entidad existente porque ya está managed
                    return systemDocumentTypeRepository.persist(existingEntity)
                            .onItem().transform(systemDocumentTypeMapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Long id) {
        return systemDocumentTypeRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() != null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(OffsetDateTime.now());
                    entity.setActive(false);
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return systemDocumentTypeRepository.persist(entity)
                            .call(() -> systemDocumentTypeRepository.flush()) // Forzar flush para asegurar que se
                            // escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> hardDelete(Long id) {
        return systemDocumentTypeRepository.deleteById(id);
    }

    @Override
    public Uni<Boolean> restore(Long id) {
        return systemDocumentTypeRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() == null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(null);
                    entity.setActive(true);
                    entity.setUpdatedAt(OffsetDateTime.now());
                    return systemDocumentTypeRepository.persist(entity)
                            .call(() -> systemDocumentTypeRepository.flush()) // Forzar flush para asegurar que se
                            // escribió en DB
                            .onItem().transform(e -> true);
                });
    }

    // ==================== SystemDocumentTypeQueryPort - Operaciones de Lectura
    // ====================

    @Override
    public Uni<Optional<SystemDocumentType>> findById(Long id) {
        return systemDocumentTypeRepository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(systemDocumentTypeMapper::toDomain));
    }

    @Override
    public Uni<Optional<SystemDocumentType>> findByIdIncludingDeleted(Long id) {
        return systemDocumentTypeRepository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(systemDocumentTypeMapper::toDomain));
    }

    @Override
    public Uni<Optional<SystemDocumentType>> findByCode(String code) {
        return systemDocumentTypeRepository.findByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(systemDocumentTypeMapper::toDomain));
    }

    @Override
    public Uni<Optional<SystemDocumentType>> findByName(String name) {
        return systemDocumentTypeRepository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(systemDocumentTypeMapper::toDomain));
    }

    // ==================== SystemDocumentTypeQueryPort - Verificaciones de
    // Existencia ====================

    @Override
    public Uni<Boolean> existsByCode(String code, Long excludeId) {
        return systemDocumentTypeRepository.existsByCode(code, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Long excludeId) {
        return systemDocumentTypeRepository.existsByName(name, excludeId);
    }

    // ==================== SystemDocumentTypeQueryPort - Listados con Paginación
    // ====================

    @Override
    public Uni<PagedResult<SystemDocumentType>> findAll(org.walrex.application.dto.query.PageRequest pageRequest, SystemDocumentTypeFilter filter) {
        // Ejecutar ambas queries en paralelo para mejor performance
        Uni<List<SystemDocumentType>> dataUni = systemDocumentTypeRepository.findWithFilters(pageRequest, filter)
                .onItem().transform(systemDocumentTypeMapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = systemDocumentTypeRepository.countWithFilters(filter);

        // Combinar ambos resultados en un PagedResult
        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(), // List<SystemDocumentType>
                        pageRequest.getPage(), // page
                        pageRequest.getSize(), // size
                        tuple.getItem2() // totalElements
                ));
    }

    @Override
    public Uni<Long> count(SystemDocumentTypeFilter filter) {
        return systemDocumentTypeRepository.countWithFilters(filter);
    }

    @Override
    public Uni<List<SystemDocumentType>> findAllWithFilter(SystemDocumentTypeFilter filter) {
        // Ordenar por priority ASC, name ASC para componentes de selección
        Sort sort = Sort.by("priority", Sort.Direction.Ascending)
                .and("name", Sort.Direction.Ascending);

        return systemDocumentTypeRepository.findAllWithFilter(filter, sort)
                .onItem().transform(entities -> entities.stream()
                        .map(systemDocumentTypeMapper::toDomain)
                        .toList());
    }

    // ==================== SystemDocumentTypeQueryPort - Streaming
    // ====================

    @Override
    public Multi<SystemDocumentType> streamAll() {
        return systemDocumentTypeRepository.streamAll()
                .onItem().transform(systemDocumentTypeMapper::toDomain);
    }

    @Override
    public Multi<SystemDocumentType> streamWithFilter(SystemDocumentTypeFilter filter) {
        // Usamos findAll con tamaño máximo y convertimos el resultado a Multi
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }

    // ==================== SystemDocumentTypeQueryPort - Consultas Especiales
    // ====================

    @Override
    public Uni<List<SystemDocumentType>> findAllDeleted() {
        return systemDocumentTypeRepository.findAllDeleted()
                .onItem().transform(entities -> entities.stream()
                        .map(systemDocumentTypeMapper::toDomain)
                        .toList());
    }
}