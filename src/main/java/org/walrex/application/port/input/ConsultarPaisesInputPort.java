package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.PaisRemesaDisponibleDto;

import java.util.List;

/**
 * Puerto de entrada para consultar países y rutas de remesas disponibles
 */
public interface ConsultarPaisesInputPort {
    /**
     * Obtiene la lista de países con sus rutas de remesas disponibles
     * Formato: Cada país incluye sus rutas en formato "MonedaOrigen → MonedaDestino (PaísDestino)"
     *
     * @return Uni con lista de países y sus rutas disponibles
     */
    Uni<List<PaisRemesaDisponibleDto>> consultarPaisesDisponibles();
}
