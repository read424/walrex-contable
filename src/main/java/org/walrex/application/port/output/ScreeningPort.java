package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ScreeningResult;

public interface ScreeningPort {

    Uni<ScreeningResult> screen(String fullName, String documentNumber);
}
