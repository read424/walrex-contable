package org.walrex.application.dto.response;

import java.time.OffsetDateTime;

/**
 * Response DTO for System Document Type.
 * This record is immutable and excludes the deletedAt field.
 */
public record SystemDocumentTypeResponse(
    Integer id,
    String code,
    String name,
    String description,
    Boolean isRequired,
    Boolean forPerson,
    Boolean forCompany,
    Integer priority,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
