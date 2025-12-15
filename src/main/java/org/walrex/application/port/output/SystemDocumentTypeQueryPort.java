package org.walrex.application.port.output;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.domain.model.PagedResult;
import org.walrex.domain.model.SystemDocumentType;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de consulta en el repositorio de tipos de
 * documento.
 */
public interface SystemDocumentTypeQueryPort {

    // Búsquedas básicas
    Uni<Optional<SystemDocumentType>> findById(Long id);

    Uni<Optional<SystemDocumentType>> findByIdIncludingDeleted(Long id);

    Uni<Optional<SystemDocumentType>> findByCode(String code);

    Uni<Optional<SystemDocumentType>> findByName(String name);

    // Verificaciones de existencia
    Uni<Boolean> existsByCode(String code, Long excludeId);

    Uni<Boolean> existsByName(String name, Long excludeId);

    // Listados con paginación
    Uni<PagedResult<SystemDocumentType>> findAll(PageRequest pageRequest, SystemDocumentTypeFilter filter);

    Uni<Long> count(SystemDocumentTypeFilter filter);

    // Streaming
    Multi<SystemDocumentType> streamAll();

    Multi<SystemDocumentType> streamWithFilter(SystemDocumentTypeFilter filter);

    // Consultas especiales
    Uni<List<SystemDocumentType>> findAllDeleted();
}
