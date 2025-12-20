package org.walrex.application.dto.command;

public record DepartamentoApplyCommand(
    String code,
    String nombre,
    String ubigeo
) {
}
