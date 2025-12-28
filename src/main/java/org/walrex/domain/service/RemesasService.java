package org.walrex.domain.service;

import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.PaisRemesaDisponibleDto;
import org.walrex.application.port.input.ConsultarPaisesInputPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.RemittanceRouteEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceRouteRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de dominio para operaciones relacionadas con remesas
 */
@Slf4j
@ApplicationScoped
public class RemesasService implements ConsultarPaisesInputPort {

    @Inject
    RemittanceRouteRepository remittanceRouteRepository;

    /**
     * Retorna la lista de países con sus rutas de remesas disponibles.
     * Consulta desde remittance_routes agrupadas por país origen.
     * Los resultados se cachean para mejorar el rendimiento.
     *
     * @return Uni con lista de países y sus rutas disponibles
     */
    @Override
    @WithSession
    @CacheResult(cacheName = "paises-remesas")
    public Uni<List<PaisRemesaDisponibleDto>> consultarPaisesDisponibles() {
        log.info("Consultando países y rutas de remesas disponibles");

        return remittanceRouteRepository.findAllActiveWithDetails()
                .map(routes -> {
                    // Agrupar rutas por país origen
                    Map<String, List<RemittanceRouteEntity>> rutasPorPais = routes.stream()
                            .collect(Collectors.groupingBy(
                                    route -> route.getRemittanceCountry().getCountry().getName()
                            ));

                    // Convertir a DTOs
                    return rutasPorPais.entrySet().stream()
                            .map(entry -> {
                                String nombrePais = entry.getKey();
                                List<String> rutasFormateadas = entry.getValue().stream()
                                        .map(this::formatearRuta)
                                        .toList();

                                return new PaisRemesaDisponibleDto(nombrePais, rutasFormateadas);
                            })
                            .toList();
                });
    }

    /**
     * Formatea una ruta en el formato: "MonedaOrigen → MonedaDestino (PaísDestino)"
     * Ejemplo: "Soles → Bolívares (Venezuela)"
     */
    private String formatearRuta(RemittanceRouteEntity route) {
        String monedaOrigen = route.getCountryCurrencyFrom().getCurrency().getName();
        String monedaDestino = route.getCountryCurrencyTo().getCurrency().getName();
        String paisDestino = route.getCountryCurrencyTo().getCountry().getName();

        return String.format("%s → %s (%s)", monedaOrigen, monedaDestino, paisDestino);
    }
}
