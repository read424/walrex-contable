package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteBeneficiaryUseCase {
    Uni<Void> delete(Long id);
}
