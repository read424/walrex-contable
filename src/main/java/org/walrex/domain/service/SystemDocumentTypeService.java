package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SystemDocumentTypeResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.SystemDocumentTypeCachePort;
import org.walrex.application.port.output.SystemDocumentTypeQueryPort;
import org.walrex.application.port.output.SystemDocumentTypeRepositoryPort;
import org.walrex.domain.exception.DuplicateSystemDocumentTypeException;
import org.walrex.domain.exception.SystemDocumentTypeNotFoundException;
import org.walrex.domain.model.SystemDocumentType;
import org.walrex.infrastructure.adapter.inbound.mapper.SystemDocumentTypeDtoMapper;
import org.walrex.infrastructure.adapter.outbound.cache.SystemDocumentTypeCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.SystemDocumentTypeCache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Transactional
@ApplicationScoped
public class SystemDocumentTypeService implements
        CreateSystemDocumentTypeUseCase,
        ListSystemDocumentTypeUseCase,
        GetSystemDocumentTypeUseCase,
        UpdateSystemDocumentTypeUseCase,
        DeleteSystemDocumentTypeUseCase,
        CheckAvailabilitySystemDocumentTypeUseCase {

    @Inject
    SystemDocumentTypeRepositoryPort systemDocumentTypeRepositoryPort;

    @Inject
    SystemDocumentTypeQueryPort systemDocumentTypeQueryPort;

    @Inject
    @SystemDocumentTypeCache
    SystemDocumentTypeCachePort systemDocumentTypeCachePort;

    @Inject
    SystemDocumentTypeDtoMapper systemDocumentTypeDtoMapper;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Crea un nuevo tipo de documento del sistema.
     */
    @Override
    public Uni<SystemDocumentType> agregar(SystemDocumentType systemDocumentType) {
        log.info("Creating system document type: {} ({})", systemDocumentType.getName(), systemDocumentType.getCode());

        // Validar unicidad de cada campo
        return validateUniqueness(systemDocumentType.getCode(), systemDocumentType.getName(), null)
                .onItem().transformToUni(v -> systemDocumentTypeRepositoryPort.save(systemDocumentType))
                .call(savedDocumentType -> {
                    // Invalidar cache después de crear
                    log.debug("Invalidating system document type cache after creation");
                    return systemDocumentTypeCachePort.invalidateAll();
                });
    }

    /**
     * Lista tipos de documento con paginación y filtros.
     *
     * Implementa cache-aside pattern:
     * 1. Intenta obtener del cache
     * 2. Si no existe, consulta la DB
     * 3. Cachea el resultado
     * 4. Devuelve el resultado
     */
    @Override
    public Uni<PagedResponse<SystemDocumentTypeResponse>> listar(PageRequest pageRequest,
                                                                 SystemDocumentTypeFilter filter) {
        log.info("Listing system document types with page: {}, size: {}, filter: {}",
                pageRequest.getPage(), pageRequest.getSize(), filter);

        // Generar clave única de cache
        String cacheKey = SystemDocumentTypeCacheKeyGenerator.generateKey(pageRequest, filter);

        // Cache-aside pattern
        return systemDocumentTypeCachePort.get(cacheKey)
                .onItem().transformToUni(cachedResult -> {
                    if (cachedResult != null) {
                        log.debug("Returning cached result for key: {}", cacheKey);
                        return Uni.createFrom().item(cachedResult);
                    }

                    // Cache miss - consultar DB
                    log.debug("Cache miss for key: {}. Querying database.", cacheKey);
                    return fetchFromDatabaseAndCache(pageRequest, filter, cacheKey);
                });
    }

    /**
     * Actualiza un tipo de documento del sistema.
     */
    @Override
    public Uni<SystemDocumentType> execute(Long id, SystemDocumentType systemDocumentType) {
        log.info("Updating system document type id: {}", id);

        // Validar unicidad excluyendo el ID actual
        return validateUniqueness(
                systemDocumentType.getCode(),
                systemDocumentType.getName(),
                id).onItem().transformToUni(v -> {
                    systemDocumentType.setId(id);
                    return systemDocumentTypeRepositoryPort.update(systemDocumentType);
                })
                .call(updatedDocumentType -> {
                    // Invalidar cache después de actualizar
                    log.debug("Invalidating system document type cache after update");
                    return systemDocumentTypeCachePort.invalidateAll();
                });
    }

    /**
     * Consulta la DB y cachea el resultado.
     */
    private Uni<PagedResponse<SystemDocumentTypeResponse>> fetchFromDatabaseAndCache(
            PageRequest pageRequest,
            SystemDocumentTypeFilter filter,
            String cacheKey) {

        return systemDocumentTypeQueryPort.findAll(pageRequest, filter)
                .onItem().transform(pagedResult -> {
                    var responses = pagedResult.content().stream()
                            .map(systemDocumentTypeDtoMapper::toResponse)
                            .toList();

                    // Convert page from 0-based (backend) to 1-based (frontend)
                    return PagedResponse.of(
                            responses,
                            pagedResult.page() + 1,
                            pagedResult.size(),
                            pagedResult.totalElements());
                })
                .call(result -> {
                    // Cachear el resultado (fire-and-forget)
                    log.debug("Caching result for key: {}", cacheKey);
                    return systemDocumentTypeCachePort.put(cacheKey, result, CACHE_TTL);
                });
    }

    /**
     * Obtiene todos los tipos de documento activos como un stream reactivo.
     */
    @Override
    public Multi<SystemDocumentTypeResponse> streamAll() {
        log.info("Streaming all active system document types");
        return systemDocumentTypeQueryPort.streamAll()
                .onItem().transform(systemDocumentTypeDtoMapper::toResponse);
    }

    /**
     * Obtiene todos los tipos de documento activos como un stream con filtros.
     */
    @Override
    public Multi<SystemDocumentTypeResponse> streamWithFilter(SystemDocumentTypeFilter filter) {
        log.info("Streaming system document types with filter: {}", filter);
        return systemDocumentTypeQueryPort.streamWithFilter(filter)
                .onItem().transform(systemDocumentTypeDtoMapper::toResponse);
    }

    /**
     * Obtiene un tipo de documento por su ID.
     */
    @Override
    public Uni<SystemDocumentType> findById(Long id) {
        log.info("Getting system document type by id: {}", id);
        return systemDocumentTypeQueryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new SystemDocumentTypeNotFoundException(id)));
    }

    /**
     * Obtiene un tipo de documento por su código.
     */
    @Override
    public Uni<SystemDocumentType> findByCode(String code) {
        log.info("Getting system document type by code: {}", code);
        return systemDocumentTypeQueryPort.findByCode(code)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new SystemDocumentTypeNotFoundException(
                                "System document type not found with code: " + code)));
    }

    /**
     * Valida que los campos únicos no existan en otros registros.
     */
    private Uni<Void> validateUniqueness(
            String code,
            String name,
            Long excludeId) {
        return systemDocumentTypeQueryPort.existsByCode(code, excludeId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateSystemDocumentTypeException("code", code));
                    }
                    return systemDocumentTypeQueryPort.existsByName(name, excludeId);
                })
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(
                                new DuplicateSystemDocumentTypeException("name", name));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<AvailabilityResponse> checkCode(String code, Long excludeId) {
        log.info("Checking code availability: {}", code);
        return systemDocumentTypeQueryPort.existsByCode(code, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("code", code, !exists));
    }

    @Override
    public Uni<AvailabilityResponse> checkName(String name, Long excludeId) {
        log.info("Checking name availability: {}", name);
        return systemDocumentTypeQueryPort.existsByName(name, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("name", name, !exists));
    }

    /**
     * Verifica disponibilidad de múltiples campos a la vez.
     */
    @Override
    public Uni<List<AvailabilityResponse>> checkAll(
            String code,
            String name,
            Long excludeId) {
        log.info("Checking availability for all fields");

        var checks = new ArrayList<Uni<AvailabilityResponse>>();

        if (code != null && !code.isBlank()) {
            checks.add(checkCode(code, excludeId));
        }
        if (name != null && !name.isBlank()) {
            checks.add(checkName(name, excludeId));
        }

        return Uni.combine().all().unis(checks).with(list -> list.stream()
                .map(obj -> (AvailabilityResponse) obj)
                .toList());
    }

    @Override
    public Uni<Boolean> deshabilitar(Long id) {
        log.info("Soft deleting system document type id: {}", id);
        return systemDocumentTypeRepositoryPort.softDelete(id)
                .call(deleted -> {
                    if (deleted) {
                        // Invalidar cache después de eliminar
                        log.debug("Invalidating system document type cache after deletion");
                        return systemDocumentTypeCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @Override
    public Uni<Boolean> habilitar(Long id) {
        log.info("Restoring system document type id: {}", id);
        return systemDocumentTypeRepositoryPort.restore(id)
                .call(restored -> {
                    if (restored) {
                        // Invalidar cache después de restaurar
                        log.debug("Invalidating system document type cache after restoration");
                        return systemDocumentTypeCachePort.invalidateAll();
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
