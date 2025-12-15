package org.walrex.domain.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SunatDocumentTypeFilter;
import org.walrex.application.dto.response.AvailabilityResponse;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SunatDocumentTypeResponse;
import org.walrex.application.port.input.*;
import org.walrex.application.port.output.SunatDocumentTypeQueryPort;
import org.walrex.application.port.output.SunatDocumentTypeRepositoryPort;
import org.walrex.domain.exception.DuplicateSunatDocumentTypeException;
import org.walrex.domain.exception.SunatDocumentTypeNotFoundException;
import org.walrex.domain.model.SunatDocumentType;
import org.walrex.infrastructure.adapter.inbound.mapper.SunatDocumentTypeDtoMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio para tipos de documentos SUNAT.
 *
 * Implementa todos los casos de uso (UseCases) y contiene la lógica de negocio.
 * Orquesta las operaciones entre los puertos de entrada y salida.
 *
 * Responsabilidades:
 * - Validaciones de negocio (unicidad, reglas de dominio)
 * - Orquestación de operaciones entre puertos
 * - Transformación de datos entre capas
 * - Manejo de transacciones
 */
@Slf4j
@Transactional
@ApplicationScoped
public class SunatDocumentTypeService implements
        CreateSunatDocumentTypeUseCase,
        ListSunatDocumentTypeUseCase,
        GetSunatDocumentTypeUseCase,
        UpdateSunatDocumentTypeUseCase,
        DeleteSunatDocumentTypeUseCase,
        CheckAvailabilitySunatDocumentTypeUseCase {

    @Inject
    SunatDocumentTypeRepositoryPort repositoryPort;

    @Inject
    SunatDocumentTypeQueryPort queryPort;

    @Inject
    SunatDocumentTypeDtoMapper dtoMapper;

    // ==================== CreateSunatDocumentTypeUseCase ====================

    /**
     * Crea un nuevo tipo de documento SUNAT.
     *
     * Valida que no existan duplicados de ID o código antes de persistir.
     */
    @Override
    public Uni<SunatDocumentType> create(SunatDocumentType documentType) {
        log.info("Creating SUNAT document type: {} (code: {})", documentType.getName(), documentType.getCode());

        // Validar unicidad de ID y código
        return validateUniqueness(documentType.getId(), documentType.getCode(), null)
                .onItem().transformToUni(v -> repositoryPort.save(documentType))
                .invoke(saved -> log.info("SUNAT document type created successfully with ID: {}", saved.getId()));
    }

    // ==================== ListSunatDocumentTypeUseCase ====================

    /**
     * Lista tipos de documentos con paginación y filtros.
     *
     * Nota: No implementa caché como Country porque los tipos de documentos
     * cambian con menos frecuencia y son pocos registros.
     */
    @Override
    public Uni<PagedResponse<SunatDocumentTypeResponse>> list(PageRequest pageRequest, SunatDocumentTypeFilter filter) {
        log.info("Listing SUNAT document types with page: {}, size: {}, filter: {}",
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
                            pagedResult.totalElements()
                    );
                });
    }

    /**
     * Obtiene todos los tipos de documentos activos como stream reactivo.
     */
    @Override
    public Multi<SunatDocumentTypeResponse> streamAll() {
        log.info("Streaming all active SUNAT document types");
        return queryPort.streamAll()
                .onItem().transform(dtoMapper::toResponse);
    }

    /**
     * Obtiene tipos de documentos como stream con filtros.
     */
    @Override
    public Multi<SunatDocumentTypeResponse> streamWithFilter(SunatDocumentTypeFilter filter) {
        log.info("Streaming SUNAT document types with filter: {}", filter);
        return queryPort.streamWithFilter(filter)
                .onItem().transform(dtoMapper::toResponse);
    }

    // ==================== GetSunatDocumentTypeUseCase ====================

    /**
     * Obtiene un tipo de documento por su ID.
     *
     * @throws SunatDocumentTypeNotFoundException si no existe
     */
    @Override
    public Uni<SunatDocumentType> findById(Integer id) {
        log.info("Getting SUNAT document type by id: {}", id);
        return queryPort.findById(id)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new SunatDocumentTypeNotFoundException(id)
                ));
    }

    /**
     * Obtiene un tipo de documento por su código SUNAT.
     *
     * @throws SunatDocumentTypeNotFoundException si no existe
     */
    @Override
    public Uni<SunatDocumentType> findByCode(String code) {
        log.info("Getting SUNAT document type by code: {}", code);
        return queryPort.findByCode(code)
                .onItem().transform(optional -> optional.orElseThrow(
                        () -> new SunatDocumentTypeNotFoundException("SUNAT document type not found with code: " + code)
                ));
    }

    // ==================== UpdateSunatDocumentTypeUseCase ====================

    /**
     * Actualiza un tipo de documento existente.
     *
     * Valida que el nuevo código no esté duplicado (excluyendo el ID actual).
     * Preserva el campo 'active' si no viene en la actualización.
     */
    @Override
    public Uni<SunatDocumentType> update(Integer id, SunatDocumentType documentType) {
        log.info("Updating SUNAT document type id: {}", id);

        // Validar unicidad del código excluyendo el ID actual
        // El ID no se valida porque no se puede cambiar
        return validateUniqueness(null, documentType.getCode(), id.toString())
                .onItem().transformToUni(v -> findById(id))
                .onItem().transformToUni(existing -> {
                    documentType.setId(id);

                    // Preservar el campo 'active' si no viene en la actualización
                    if (documentType.getActive() == null) {
                        documentType.setActive(existing.getActive());
                        log.debug("Preserving active status: {}", existing.getActive());
                    }

                    return repositoryPort.update(documentType);
                })
                .invoke(updated -> log.info("SUNAT document type updated successfully: {}", id));
    }

    // ==================== DeleteSunatDocumentTypeUseCase ====================

    /**
     * Desactiva un tipo de documento (marca active=false).
     */
    @Override
    public Uni<Boolean> deactivate(Integer id) {
        log.info("Deactivating SUNAT document type id: {}", id);
        return repositoryPort.deactivate(id)
                .invoke(deactivated -> {
                    if (deactivated) {
                        log.info("SUNAT document type deactivated successfully: {}", id);
                    } else {
                        log.warn("SUNAT document type not found or already inactive: {}", id);
                    }
                });
    }

    /**
     * Reactiva un tipo de documento (marca active=true).
     */
    @Override
    public Uni<Boolean> activate(Integer id) {
        log.info("Activating SUNAT document type id: {}", id);
        return repositoryPort.activate(id)
                .invoke(activated -> {
                    if (activated) {
                        log.info("SUNAT document type activated successfully: {}", id);
                    } else {
                        log.warn("SUNAT document type not found or already active: {}", id);
                    }
                });
    }

    // ==================== CheckAvailabilitySunatDocumentTypeUseCase ====================

    /**
     * Verifica si un ID está disponible.
     */
    @Override
    public Uni<AvailabilityResponse> checkId(Integer id, String excludeId) {
        log.info("Checking ID availability: {}", id);
        return queryPort.existsById(id, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("id", id.toString(), !exists));
    }

    /**
     * Verifica si un código SUNAT está disponible.
     */
    @Override
    public Uni<AvailabilityResponse> checkCode(String code, String excludeId) {
        log.info("Checking code availability: {}", code);
        return queryPort.existsByCode(code, excludeId)
                .onItem().transform(exists -> AvailabilityResponse.of("code", code, !exists));
    }

    /**
     * Verifica disponibilidad de múltiples campos a la vez.
     */
    @Override
    public Uni<List<AvailabilityResponse>> checkAll(Integer id, String code, String excludeId) {
        log.info("Checking availability for all fields");

        var checks = new ArrayList<Uni<AvailabilityResponse>>();

        if (id != null) {
            checks.add(checkId(id, excludeId));
        }
        if (code != null && !code.isBlank()) {
            checks.add(checkCode(code, excludeId));
        }

        return Uni.combine().all().unis(checks).with(list ->
                list.stream()
                        .map(obj -> (AvailabilityResponse) obj)
                        .toList()
        );
    }

    // ==================== Métodos Privados de Validación ====================

    /**
     * Valida que los campos únicos no existan en otros registros.
     *
     * @param id ID a validar (puede ser null si no se valida)
     * @param code Código a validar (puede ser null si no se valida)
     * @param excludeId ID a excluir de la validación (útil para updates)
     * @return Uni<Void> que falla si encuentra duplicados
     */
    private Uni<Void> validateUniqueness(Integer id, String code, String excludeId) {
        Uni<Void> result = Uni.createFrom().voidItem();

        // Validar ID si se proporciona
        if (id != null) {
            result = result.onItem().transformToUni(v ->
                    queryPort.existsById(id, excludeId)
                            .onItem().transformToUni(exists -> {
                                if (exists) {
                                    return Uni.createFrom().failure(
                                            new DuplicateSunatDocumentTypeException("id", id.toString()));
                                }
                                return Uni.createFrom().voidItem();
                            })
            );
        }

        // Validar código si se proporciona
        if (code != null && !code.isBlank()) {
            result = result.onItem().transformToUni(v ->
                    queryPort.existsByCode(code, excludeId)
                            .onItem().transformToUni(exists -> {
                                if (exists) {
                                    return Uni.createFrom().failure(
                                            new DuplicateSunatDocumentTypeException("code", code));
                                }
                                return Uni.createFrom().voidItem();
                            })
            );
        }

        return result;
    }
}
