package org.walrex.application.dto.command;

public record ProvinciaApplyCommand(
        String code,
        String nombre,
        String ubigeo
) {
}
