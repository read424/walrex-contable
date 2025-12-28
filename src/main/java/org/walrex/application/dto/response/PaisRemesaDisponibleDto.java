package org.walrex.application.dto.response;

import java.util.List;

/**
 * DTO que representa un país con sus rutas de remesas disponibles
 *
 * @param nombrePais Nombre del país origen
 * @param rutasDisponibles Lista de rutas en formato "MonedaOrigen → MonedaDestino (PaísDestino)"
 */
public record PaisRemesaDisponibleDto(
        String nombrePais,
        List<String> rutasDisponibles
) {
}
