package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SunatDocumentType;

public interface UpdateSunatDocumentTypeUseCase {
    Uni<SunatDocumentType> update(Integer id, SunatDocumentType documentType);
}
