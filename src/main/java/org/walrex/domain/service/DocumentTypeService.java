package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.dto.response.DocumentTypeResponse;
import org.walrex.application.port.input.GetDocumentTypesUseCase;
import org.walrex.application.port.output.DocumentTypeQueryPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DocumentTypeIdEntity;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class DocumentTypeService implements GetDocumentTypesUseCase {

    @Inject
    DocumentTypeQueryPort queryPort;

    @Override
    public Uni<List<DocumentTypeResponse>> getDocumentTypesByCountry(String countryIso2) {
        log.debug("Fetching document types for country: {}", countryIso2);
        
        return queryPort.findByCountryIso2(countryIso2)
                .map(entities -> entities.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()));
    }

    private DocumentTypeResponse mapToResponse(DocumentTypeIdEntity entity) {
        return DocumentTypeResponse.builder()
                .id_document_type(entity.getId())
                .code(entity.getSigla())
                .name(entity.getDetName())
                .build();
    }
}
