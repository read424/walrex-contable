package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.query.SystemDocumentTypeFilter;
import org.walrex.application.dto.response.SystemDocumentTypeSelectResponse;

import java.util.List;

/**
 * Puerto de entrada para obtener todos los tipos de documento sin paginación.
 * Optimizado para componentes de selección (dropdown, select, autocomplete).
 */
public interface GetAllSystemDocumentTypesUseCase {
    /**
     * Obtiene todos los tipos de documento aplicando filtros opcionales.
     * Los resultados se ordenan por priority ASC, name ASC.
     *
     * @param filter Filtros opcionales (active, forPerson, forCompany, search, etc.)
     * @return Lista de tipos de documento con campos esenciales
     */
    Uni<List<SystemDocumentTypeSelectResponse>> execute(SystemDocumentTypeFilter filter);
}
