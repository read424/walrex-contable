package org.walrex.domain.service;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.ConsultarPaisesInputPort;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CountryRepository;

import java.util.List;

/**
 * Servicio de dominio para operaciones relacionadas con remesas
 */
@Slf4j
@ApplicationScoped
public class RemesasService implements ConsultarPaisesInputPort {

    @Inject
    CountryRepository countryRepository;

    /**
     * Retorna la lista de países hacia los cuales se pueden realizar remesas.
     * Los resultados se cachean para mejorar el rendimiento.
     *
     * @return Lista de nombres de países disponibles
     */
    @Override
    @CacheResult(cacheName = "paises-remesas")
    public List<String> ejecutar() {
        log.info("Consultando países disponibles para remesas");

        // Obtener todos los países activos y extraer solo los nombres
        // Se usa await().indefinitely() porque MCP tools esperan respuesta síncrona
        return countryRepository.streamAll()
                .map(country -> country.getName())
                .collect()
                .asList()
                .await()
                .indefinitely();
    }
}
