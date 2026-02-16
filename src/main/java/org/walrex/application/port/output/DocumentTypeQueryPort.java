package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DocumentTypeIdEntity;

import java.util.List;

public interface DocumentTypeQueryPort {
    Uni<List<DocumentTypeIdEntity>> findByCountryIso2(String countryIso2);
}
