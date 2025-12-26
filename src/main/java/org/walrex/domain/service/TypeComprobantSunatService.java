package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.TypeComprobantSunatSelectResponse;
import org.walrex.application.port.input.GetAllTypeComprobantsSunatUseCase;
import org.walrex.application.port.output.TypeComprobantSunatQueryPort;
import org.walrex.infrastructure.adapter.inbound.mapper.TypeComprobantSunatDtoMapper;

import java.util.List;

@Slf4j
@ApplicationScoped
public class TypeComprobantSunatService implements GetAllTypeComprobantsSunatUseCase {

    @Inject
    TypeComprobantSunatQueryPort queryPort;

    @Inject
    TypeComprobantSunatDtoMapper dtoMapper;

    /**
     * Obtiene todos los tipos de comprobantes SUNAT.
     * Los resultados se ordenan por código SUNAT.
     */
    @Override
    public Uni<List<TypeComprobantSunatSelectResponse>> execute() {
        log.info("Getting all type comprobants SUNAT");

        return queryPort.findAll()
                .onItem().transform(dtoMapper::toSelectResponseList);
    }
}
