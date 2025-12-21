package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.ProvinceFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.ProvinceResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.ProvinceQueryPort;
import org.walrex.application.port.output.ProvinceRepositoryPort;
import org.walrex.domain.exception.DuplicateProvinceException;
import org.walrex.domain.exception.ProvinceNotFoundException;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.inbound.mapper.ProvinceDtoMapper;

@Slf4j
@Transactional
@ApplicationScoped
public class ProvinceService implements
        CreateProvinceUseCase,
        UpdateProvinceUseCase,
        DeleteProvinceUseCase,
        GetProvinceUseCase,
        ListProvinceRegionalUseCase {

    @Inject
    ProvinceRepositoryPort repositoryPort;

    @Inject
    ProvinceQueryPort queryPort;

    @Inject
    ProvinceDtoMapper dtoMapper;

    @Inject
    GetDepartmentRegionalUseCase getDepartmentUseCase;


    @Override
    public Uni<PagedResponse<ProvinceResponse>> listar(PageRequest pageRequest, ProvinceFilter filter) {
        log.info("Listing provincias with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        return queryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(dtoMapper::toResponse)
                            .toList();

                    // Convert page from 0-based (backend) to 1-based (frontend)
                    return PagedResponse.of(
                            responses,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements());
                });
    }

    @Override
    public Multi<ProvinceResponse> streamAll() {
        return queryPort.streamAll()
                .onItem().transform(dtoMapper::toResponse);
    }

    @Override
    public Multi<ProvinceResponse> streamWithFilter(ProvinceFilter filter) {
        return queryPort.streamWithFilter(filter)
                .onItem().transform(dtoMapper::toResponse);
    }

    @Override
    public Uni<Province> agregar(Province province) {
        log.info("Creating province: {} ({}) for department: {}",
                province.getName(), province.getCode(), province.getDepartament().getId());

        Integer departmentId = province.getDepartament().getId();

        // Cargar el departamento completo antes de validar y guardar
        return getDepartmentUseCase.findById(departmentId)
                .onItem().transformToUni(departament -> {
                    province.setDepartament(departament);
                    return validateUniqueness(province.getCode(), province.getName(), departmentId, null)
                            .onItem().transformToUni(v -> repositoryPort.save(province));
                });
    }

    @Override
    public Uni<Province> execute(Integer id, Province province) {
        log.info("Updating province id: {}", id);

        // Cargar la provincia existente para obtener el departamento completo
        return queryPort.findById(id)
                .onItem().transformToUni(existingProvince -> {
                    Province existing = existingProvince.orElseThrow(() -> new ProvinceNotFoundException(id));
                    Integer departmentId = existing.getDepartament().getId();

                    return validateUniqueness(province.getCode(), province.getName(), departmentId, id)
                            .onItem().transformToUni(v -> {
                                province.setId(id);
                                province.setDepartament(existing.getDepartament());
                                return repositoryPort.update(province);
                            });
                });
    }

    @Override
    public Uni<Void> deshabilitar(Integer id) {
        log.info("Soft deleting province id: {}", id);
        return repositoryPort.softDelete(id)
                .replaceWithVoid();
    }

    @Override
    public Uni<Province> findById(Integer id) {
        return queryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProvinceNotFoundException(id)));
    }

    @Override
    public Uni<Province> findByCode(String code) {
        return queryPort.findByCode(code)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProvinceNotFoundException("code", code)));
    }

    @Override
    public Uni<Province> findByName(String name) {
        return queryPort.findByName(name)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new ProvinceNotFoundException("name", name)));
    }

    private Uni<Void> validateUniqueness(String code, String name, Integer idDepartment, Integer excludeId) {
        return queryPort.existsByCode(code, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProvinceException("code", code));
                    }
                    return queryPort.existsByNameForDepartment(name, idDepartment, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateProvinceException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
