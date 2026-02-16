package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.DocumentTypeResponse;

import java.util.List;

public interface GetDocumentTypesUseCase {
    Uni<List<DocumentTypeResponse>> getDocumentTypesByCountry(String countryIso2);
}
