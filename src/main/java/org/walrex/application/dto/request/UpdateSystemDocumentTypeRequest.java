package org.walrex.application.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating an existing System Document Type.
 */
public record UpdateSystemDocumentTypeRequest(

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must contain only uppercase letters, numbers, underscores and hyphens")
    String code,

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    Boolean isRequired,

    Boolean forPerson,

    Boolean forCompany,

    @Min(value = 0, message = "Priority must be greater than or equal to 0")
    Integer priority,

    Boolean active
) {
}
