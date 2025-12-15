package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteSunatDocumentTypeUseCase {

    public Uni<Boolean> deactivate(Integer id);

    Uni<Boolean> activate(Integer id);
}
