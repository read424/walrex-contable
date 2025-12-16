package org.walrex.application.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de respuesta para un cliente individual.
 *
 * Nota: Usamos record (inmutable) para DTOs.
 * No exponemos deletedAt al exterior por seguridad.
 */
public record CustomerResponse(
        Integer id,
        Integer idTypeDocument,
        String numberDocument,
        String lastName,
        String firstName,
        String gender,
        String email,
        LocalDate birthDate,
        Integer idProfessional,
        String isPEP,
        Integer idCountryResidence,
        String phoneNumber,
        Integer idCountryPhone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
