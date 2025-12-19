package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.command.UbigeoImportApplyCommand;
import org.walrex.application.dto.response.UbigeoImportApplyResultResponse;

public interface ApplyUbigeoImportUseCase {
    Uni<UbigeoImportApplyResultResponse> execute(UbigeoImportApplyCommand command);
}
