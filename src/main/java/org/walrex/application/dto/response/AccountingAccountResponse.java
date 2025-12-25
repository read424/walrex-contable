package org.walrex.application.dto.response;

import org.walrex.domain.model.AccountType;
import org.walrex.domain.model.NormalSide;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para una cuenta contable individual.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record AccountingAccountResponse(
        Integer id,
        String code,
        String name,
        AccountType type,
        NormalSide normalSide,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
