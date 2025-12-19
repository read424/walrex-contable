package org.walrex.application.dto.command;

public record DistritoApplyCommand(
    String code,
    String nombre,
    String ubigeo
) {
}
