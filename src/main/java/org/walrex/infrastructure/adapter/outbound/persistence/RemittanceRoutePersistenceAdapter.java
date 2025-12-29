package org.walrex.infrastructure.adapter.outbound.persistence;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.RemittanceRouteOutputPort;
import org.walrex.domain.model.RemittanceRoute;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.RemittanceRouteMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.RemittanceRouteRepository;

import java.util.*;

/**
 * Adaptador de persistencia para consultar rutas de remesas configuradas
 */
@Slf4j
@ApplicationScoped
public class RemittanceRoutePersistenceAdapter implements RemittanceRouteOutputPort {

    @Inject
    RemittanceRouteRepository remittanceRouteRepository;

    @Inject
    RemittanceRouteMapper remittanceRouteMapper;

    @Override
    @WithSession
    public Uni<List<RemittanceRoute>> findAllActiveRoutes() {
        log.info("Fetching all active remittance routes");

        return remittanceRouteRepository.findAllActiveWithDetails()
                .onItem().transform(entities -> {
                    if (entities.isEmpty()) {
                        log.info("No active remittance routes found");
                        return new ArrayList<RemittanceRoute>();
                    }

                    log.info("Found {} active remittance route entities", entities.size());

                    // Usar el mapper para convertir entidades a dominio
                    List<RemittanceRoute> routes = remittanceRouteMapper.toDomainList(entities);

                    log.info("Mapped {} remittance routes", routes.size());
                    return routes;
                });
    }
}
