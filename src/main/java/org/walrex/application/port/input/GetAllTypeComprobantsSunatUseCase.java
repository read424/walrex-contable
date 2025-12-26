package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.TypeComprobantSunatSelectResponse;

import java.util.List;

/**
 * Puerto de entrada para obtener todos los tipos de comprobantes SUNAT sin paginación.
 * Optimizado para componentes de selección (dropdown, select, autocomplete).
 */
public interface GetAllTypeComprobantsSunatUseCase {
    /**
     * Obtiene todos los tipos de comprobantes SUNAT.
     * Los resultados se ordenan por sunatCode ASC.
     *
     * @return Lista de tipos de comprobantes con campos esenciales
     */
    Uni<List<TypeComprobantSunatSelectResponse>> execute();
}
