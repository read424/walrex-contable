package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.application.port.output.ProvinceQueryPort;
import org.walrex.application.port.output.ProvinceRepositoryPort;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProvinceEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProvinceMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProvinceRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class ProvincePersistenceAdapter implements ProvinceRepositoryPort, ProvinceQueryPort {

    @Inject
    ProvinceRepository provinceRepository;

    @Inject
    ProvinceMapper provinceMapper;

    @Override
    public Uni<PagedResult<Province>> findAll(PageRequest pageRequest, ProvinceFilter filter) {
        Uni<List<Province>> dataUni = provinceRepository.findWithFilters(pageRequest, filter)
                .onItem().transform(provinceMapper::toDomain)
                .collect().asList();

        Uni<Long> countUni = provinceRepository.countWithFilters(filter);

        return Uni.combine().all().unis(dataUni, countUni)
                .asTuple()
                .onItem().transform(tuple -> PagedResult.of(
                        tuple.getItem1(),
                        pageRequest.getPage(),
                        pageRequest.getSize(),
                        tuple.getItem2()));
    }

    @Override
    public Uni<Optional<Province>> findById(Integer id) {
        return provinceRepository.findActiveById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(provinceMapper::toDomain));
    }

    @Override
    public Uni<Optional<Province>> findByIdIncludingDeleted(Integer id) {
        return provinceRepository.findById(id)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(provinceMapper::toDomain));
    }

    @Override
    public Uni<Optional<Province>> findByCode(String code) {
        return provinceRepository.findActiveByCode(code)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(provinceMapper::toDomain));
    }

    @Override
    public Uni<Optional<Province>> findByName(String name) {
        return provinceRepository.findByName(name)
                .onItem().transform(entity -> Optional.ofNullable(entity).map(provinceMapper::toDomain));
    }

    @Override
    public Uni<Boolean> existsByCode(String code, Integer excludeId) {
        return provinceRepository.existsByCode(code, excludeId);
    }

    @Override
    public Uni<Boolean> existsByNameForDepartment(String name, Integer idDepartment, Integer excludeId) {
        return provinceRepository.existsByNameForDepartment(name, idDepartment, excludeId);
    }

    @Override
    public Uni<Long> count(ProvinceFilter filter) {
        return provinceRepository.countWithFilters(filter);
    }

    @Override
    public Uni<Long> countByDepartment(Integer idDepartment) {
        return provinceRepository.count("departament.id = ?1 and status = true", idDepartment);
    }

    @Override
    public Multi<Province> findByDepartment(Integer idDepartment) {
        return provinceRepository.findByIdDepartamento(idDepartment)
                .onItem().transform(provinceMapper::toDomain);
    }

    @Override
    public Multi<Province> streamAll() {
        return provinceRepository.streamAll()
                .onItem().transform(provinceMapper::toDomain);
    }

    @Override
    public Multi<Province> streamWithFilter(ProvinceFilter filter) {
        return provinceRepository.findWithFilters(PageRequest.builder().page(0).size(Integer.MAX_VALUE).build(), filter)
                .onItem().transform(provinceMapper::toDomain);
    }

    // ==================== ProvinceRepositoryPort Methods ====================

    @Override
    public Uni<Province> save(Province province) {
        ProvinceEntity entity = provinceMapper.toEntity(province);
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setStatus(true);

        return provinceRepository.persist(entity)
                .onItem().transform(provinceMapper::toDomain);
    }

    @Override
    public Uni<Province> update(Province province) {
        return provinceRepository.findById(province.getId())
                .onItem().transformToUni(existingEntity -> {
                    if (existingEntity == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("Province not found with id: " + province.getId()));
                    }

                    existingEntity.setCodigo(province.getCode());
                    existingEntity.setName(province.getName());
                    existingEntity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    // Note: departament and status usually unchanged on update unless specific API

                    return provinceRepository.persist(existingEntity)
                            .onItem().transform(provinceMapper::toDomain);
                });
    }

    @Override
    public Uni<Boolean> softDelete(Integer id) {
        return provinceRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || !entity.getStatus()) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setStatus(false);
                    entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    return provinceRepository.persist(entity)
                            .call(() -> provinceRepository.flush())
                            .onItem().transform(e -> true);
                });
    }

    @Override
    public Uni<Boolean> hardDelete(Integer id) {
        return provinceRepository.deleteById(id);
    }

    @Override
    public Uni<Boolean> restore(Integer id) {
        return provinceRepository.findById(id)
                .onItem().transformToUni(entity -> {
                    if (entity == null || entity.getStatus()) {
                        return Uni.createFrom().item(false);
                    }
                    entity.setStatus(true);
                    entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    return provinceRepository.persist(entity)
                            .call(() -> provinceRepository.flush())
                            .onItem().transform(e -> true);
                });
    }
}
