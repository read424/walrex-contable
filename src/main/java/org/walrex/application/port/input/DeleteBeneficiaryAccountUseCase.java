package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteBeneficiaryAccountUseCase {
    Uni<Void> delete(Long id);
}
