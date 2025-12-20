package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.port.output.DepartamentQueryPort;
import org.walrex.application.port.output.DepartamentRepositoryPort;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.PagedResult;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.DepartamentMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.DepartamentRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DepartamentPersistenceAdapter implements DepartamentRepositoryPort, DepartamentQueryPort {

    @Inject
    DepartamentRepository departamentRepository;

    @Inject
    DepartamentMapper departamentMapper;

    @Override
    public Uni<Departament> save(Departament departament) {
        return departamentRepository.persist(departamentMapper.toEntity(departament))
                .onItem().transform(departamentMapper::toDomain);
    }

    @Override
    public Uni<Departament> update(Departament departament) {
        return departamentRepository.findById(departament.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Departament not found with id: " + departament.getId()));
                    }

                    existingEntity.setCodigo(departament.getCode());
                    existingEntity.setNombre(departament.getName());
                    // status usually unchanged on update unless specific api
                    existingEntity.setUdpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                    return departamentRepository.persist(existingEntity)
                            .onItem().transform(departamentMapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        return departamentRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() != null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(OffsetDateTime.now());
                    entity.setStatus(false);
                    entity.setUdpdatedAt(OffsetDateTime.now());
                    return departamentRepository.persist(entity)
                            .call(() -> departamentRepository.flush())
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> hardDelete(Integer id) {
        return departamentRepository.deleteById(id);
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        return departamentRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getDeletedAt() == null) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setDeletedAt(null);
                    entity.setStatus(true);
                    entity.setUdpdatedAt(OffsetDateTime.now());
                    return departamentRepository.persist(entity)
                            .call(() -> departamentRepository.flush())
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Optional<Departament>> findById(Integer id) {
        return departamentRepository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(departamentMapper::toDomain));
    }

    @Override
    public Uni<Optional<Departament>> findByIdIncludingDeleted(Integer id) {
        return departamentRepository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(departamentMapper::toDomain));
    }

    @Override
    public Uni<Optional<Departament>> findByCode(String code) {
        return departamentRepository.findActiveByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(departamentMapper::toDomain));
    }

    @Override
    public Uni<Optional<Departament>> findByName(String name) {
        return departamentRepository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(departamentMapper::toDomain));
    }

    @Override
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        return departamentRepository.existsByCode(code, excludeId);
    }

    @Override
    public Uni<Boolean> existsByName(String name, Integer excludeId) {
        return departamentRepository.existsByName(name, excludeId);
    }

    @Override
    public Uni<PagedResult<Departament>> findAll(PageRequest pageRequest, DepartamentFilter filter) {
        Uni<List<Departament>> dataUni = departamentRepository.findWithFilters(pageRequest, filter)
                .onItem().transform(departamentMapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = departamentRepository.countWithFilters(filter);

        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),
                        pageRequest.getPage(),
                        pageRequest.getSize(),
                        tuple.getItem2()));
    }

    @Override
    public Uni<Long> count(DepartamentFilter filter) {
        return departamentRepository.countWithFilters(filter);
    }

    @Override
    public Multi<Departament> streamAll() {
        return departamentRepository.streamAll()
                .onItem().transform(departamentMapper::toDomain);
    }

    @Override
    public Multi<Departament> streamWithFilter(DepartamentFilter filter) {
        return findAll(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transformToMulti(pagedResult -> Multi.createFrom().iterable(pagedResult.content()));
    }
}
