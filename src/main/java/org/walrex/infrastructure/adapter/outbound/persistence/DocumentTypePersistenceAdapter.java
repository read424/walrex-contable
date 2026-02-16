package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.application.port.output.DocumentTypeQueryPort;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DocumentTypeIdEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.DocumentTypeIdRepository;

import java.util.List;

@ApplicationScoped
public class DocumentTypePersistenceAdapter implements DocumentTypeQueryPort {

    @Inject
    DocumentTypeIdRepository repository;

    @Override
    public Uni<List<DocumentTypeIdEntity>> findByCountryIso2(String countryIso2) {
        return repository.findByCountryIso2(countryIso2);
    }
}
