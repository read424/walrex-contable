package org.walrex.domain.service;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.request.OcupacionCreateRequest;
import org.walrex.application.dto.request.OcupacionUpdateRequest;
import org.walrex.application.dto.response.OcupacionResponse;
import org.walrex.application.port.input.OcupacionUseCase;
import org.walrex.application.port.output.CachePort;
import org.walrex.application.port.output.OcupacionRepositoryPort;
import org.walrex.domain.exception.DuplicateOcupacionException;
import org.walrex.domain.exception.OcupacionNotFoundException;
import org.walrex.domain.model.Ocupacion;
import org.walrex.infrastructure.adapter.inbound.mapper.OcupacionRestMapper;
import org.walrex.infrastructure.adapter.outbound.cache.OcupacionCacheKeyGenerator;
import org.walrex.infrastructure.adapter.outbound.cache.qualifier.OcupacionCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class OcupacionDomainService implements OcupacionUseCase {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Inject
    OcupacionRepositoryPort ocupacionRepositoryPort;

    @Inject
    @OcupacionCache
    CachePort<OcupacionResponse> cachePort;

    @Inject
    OcupacionCacheKeyGenerator cacheKeyGenerator;

    @Inject
    OcupacionRestMapper ocupacionRestMapper;

    @Override
    @WithTransaction
    public Uni<Ocupacion> createOcupacion(OcupacionCreateRequest request) {
        log.debug("Creating ocupacion with request: {}", request);
        Ocupacion ocupacion = ocupacionRestMapper.toDomain(request);
        return validateUniqueness(ocupacion.getCodigo(), ocupacion.getNombre(), null)
                .onItem().transformToUni(v -> {
                    ocupacion.setStatus(1); // Default status
                    ocupacion.setCreatedAt(LocalDateTime.now());
                    ocupacion.setUpdatedAt(LocalDateTime.now());
                    return ocupacionRepositoryPort.save(ocupacion)
                            .call(savedOcupacion -> cachePort.invalidateAll());
                });
    }

    @Override
    @WithTransaction
    public Uni<Ocupacion> updateOcupacion(Long id, Ocupacion ocupacion) {
        log.debug("Updating ocupacion with ID: {} and data: {}", id, ocupacion);
        
        return validateUniqueness(ocupacion.getCodigo(), ocupacion.getNombre(), id)
                .onItem().transformToUni(v -> {
                    ocupacion.setId(id);
                    ocupacion.setUpdatedAt(LocalDateTime.now());
                    return ocupacionRepositoryPort.update(ocupacion)
                            .call(updatedOcupacion -> cachePort.invalidateAll());
                });
    }

    @Override
    @WithTransaction
    public Uni<Void> deleteOcupacion(Long id) {
        log.debug("Deleting ocupacion with ID: {}", id);
        return ocupacionRepositoryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(() -> new OcupacionNotFoundException("Ocupacion con ID " + id + " no encontrada.")))
                .flatMap(ocupacionToDelete -> {
                    ocupacionToDelete.setStatus(0); // Logical delete
                    ocupacionToDelete.setUpdatedAt(LocalDateTime.now());
                    return ocupacionRepositoryPort.update(ocupacionToDelete)
                            .call(updatedOcupacion -> cachePort.invalidateAll())
                            .replaceWithVoid();
                });
    }

    @Override
    public Uni<Ocupacion> findOcupacionById(Long id) {
        log.debug("Finding ocupacion by ID: {}", id);
        return ocupacionRepositoryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(() -> new OcupacionNotFoundException("Ocupacion con ID " + id + " no encontrada.")));
    }

    @Override
    public Uni<List<OcupacionResponse>> findAllOcupaciones(Integer page, Integer size, String nombreFilter) {
        log.debug("Finding all ocupaciones with page: {}, size: {}, nameFilter: {}", page, size, nombreFilter);
        String cacheKey = cacheKeyGenerator.generateKeyForFindAllPaginated(page, size, nombreFilter);
        
        return cachePort.<OcupacionResponse>getList(cacheKey)
                .onItem().ifNull().switchTo(() ->
                    ocupacionRepositoryPort.findAll(page, size, nombreFilter)
                        .map(ocupaciones -> ocupaciones.stream()
                                .map(ocupacionRestMapper::toResponse)
                                .collect(Collectors.toList()))
                        .call(ocupacionResponses -> {
                            if (!ocupacionResponses.isEmpty()) {
                                return cachePort.putList(cacheKey, ocupacionResponses, CACHE_TTL);
                            }
                            return Uni.createFrom().voidItem();
                        })
                );
    }

    @Override
    @WithSession
    public Uni<List<OcupacionResponse>> findAllOcupacionesNoPaginated(String nombreFilter) {
        log.debug("Finding all ocupaciones (no paginated) with nameFilter: {}", nombreFilter);
        String cacheKey = cacheKeyGenerator.generateKeyForFindAllNoPaginated(nombreFilter);

        return cachePort.<OcupacionResponse>getList(cacheKey)
                .onItem().ifNull().switchTo(() ->
                    ocupacionRepositoryPort.findAllNoPaginated(nombreFilter)
                        .map(ocupaciones -> ocupaciones.stream()
                                .map(ocupacionRestMapper::toResponse)
                                .collect(Collectors.toList()))
                            .call(ocupacionResponses -> {
                                if (!ocupacionResponses.isEmpty()) {
                                    return cachePort.putList(cacheKey, ocupacionResponses, CACHE_TTL);
                                }
                                return Uni.createFrom().voidItem();
                            })
                );
    }

    private Uni<Void> validateUniqueness(String codigo, String nombre, Long excludeId) {
        return ocupacionRepositoryPort.findByCodigo(codigo)
                .onItem().transformToUni(optional -> {
                    if (optional.isPresent() && (excludeId == null || !optional.get().getId().equals(excludeId))) {
                        return Uni.createFrom().failure(new DuplicateOcupacionException("Ocupacion con código " + codigo + " ya existe."));
                    }
                    return ocupacionRepositoryPort.findByNombre(nombre);
                })
                .onItem().transformToUni(optional -> {
                    if (optional.isPresent() && (excludeId == null || !optional.get().getId().equals(excludeId))) {
                        return Uni.createFrom().failure(new DuplicateOcupacionException("Ocupacion con nombre " + nombre + " ya existe."));
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
