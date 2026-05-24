package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ClientScreeningHistory;

public interface ScreeningHistoryRepositoryPort {

    Uni<Void> save(ClientScreeningHistory history);
}
