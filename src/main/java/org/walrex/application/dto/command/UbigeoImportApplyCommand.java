package org.walrex.application.dto.command;

import java.util.List;

public record UbigeoImportApplyCommand(
    List<DepartamentoApplyCommand> departamento,
    List<ProvinciaApplyCommand> provincia,
    List<DistritoApplyCommand> distrito
) {
}
