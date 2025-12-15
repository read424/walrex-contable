package org.walrex.application.port.input;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.PageRequest;
import org.walrex.application.dto.query.SunatDocumentTypeFilter;
import org.walrex.application.dto.response.PagedResponse;
import org.walrex.application.dto.response.SunatDocumentTypeResponse;

/**
 * Caso de uso para listar tipos de documentos SUNAT.
 *
 * Soporta paginación, ordenamiento y filtros.
 */
public interface ListSunatDocumentTypeUseCase {
    /**
     * Lista tipos de documentos con paginación y filtros.
     *
     * @param pageRequest Configuración de paginación (page, size, sort)
     * @param filter Filtros opcionales (search, code, active, etc.)
     * @return Uni con respuesta paginada
     */
    Uni<PagedResponse<SunatDocumentTypeResponse>> list(PageRequest pageRequest, SunatDocumentTypeFilter filter);

    /**
     * Obtiene todos los tipos de documentos como stream reactivo.
     *
     * @return Multi que emite cada tipo de documento individualmente
     */
    Multi<SunatDocumentTypeResponse> streamAll();

    /**
     * Obtiene tipos de documentos como stream con filtros aplicados.
     *
     * @param filter Filtros a aplicar
     * @return Multi que emite cada tipo de documento que cumple los filtros
     */
    Multi<SunatDocumentTypeResponse> streamWithFilter(SunatDocumentTypeFilter filter);
}
