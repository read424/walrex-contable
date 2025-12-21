package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.DepartamentFilter;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.response.DepartamentResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.DepartamentQueryPort;
import org.walrex.application.port.output.DepartamentRepositoryPort;
import org.walrex.domain.exception.DepartamentNotFoundException;
import org.walrex.domain.exception.DuplicateDepartamentException;
import org.walrex.domain.model.Departament;
import org.walrex.infrastructure.adapter.inbound.mapper.DepartamentDtoMapper;

@Slf4j
@Transactional
@ApplicationScoped
public class DepartamentService implements
        CreateDepartamentUseCase,
        UpdateDepartamentUseCase,
        DeleteDepartamentUseCase,
        GetDepartmentRegionalUseCase,
        ListDepartmentRegionalUseCase  {

    @Inject
    DepartamentRepositoryPort repositoryPort;

    @Inject
    DepartamentQueryPort queryPort;

    @Inject
    DepartamentDtoMapper dtoMapper;

    @Override
    public Uni<Departament> agregar(Departament departament) {
        log.info("Creating departament: {} ({})", departament.getName(), departament.getCode());
        return validateUniqueness(departament.getCode(), departament.getName(), null)
                .onItem().transformToUni(v -> repositoryPort.save(departament));
    }

    @Override
    public Uni<Departament> execute(Integer id, Departament departament) {
        log.info("Updating departament id: {}", id);
        return validateUniqueness(departament.getCode(), departament.getName(), id)
                .onItem().transformToUni(v -> {
                    departament.setId(id);
                    return repositoryPort.update(departament);
                });
    }

    @Override
    public Uni<Void> deshabilitar(Integer id) {
        log.info("Soft deleting departament id: {}", id);
        return repositoryPort.softDelete(id)
                .replaceWithVoid();
    }

    @Override
    public Uni<Departament> findById(Integer id) {
        return queryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new DepartamentNotFoundException(id)));
    }

    @Override
    public Uni<PagedResponse<DepartamentResponse>> listar(PageRequest pageRequest, DepartamentFilter filter) {
        log.info("Listing departaments with page: {}, size: {}, filter: {}",
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
    public Multi<DepartamentResponse> streamAll() {
        return queryPort.streamAll()
                .onItem().transform(dtoMapper::toResponse);
    }

    @Override
    public Multi<DepartamentResponse> streamWithFilter(DepartamentFilter filter) {
        return queryPort.streamWithFilter(filter)
                .onItem().transform(dtoMapper::toResponse);
    }

    private Uni<Void> validateUniqueness(String code, String name, Integer excludeId) {
        return queryPort.existsByCode(code, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateDepartamentException("code", code));
                    }
                    return queryPort.existsByName(name, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateDepartamentException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
