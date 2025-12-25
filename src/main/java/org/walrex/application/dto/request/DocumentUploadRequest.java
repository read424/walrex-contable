package org.walrex.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for uploading a document as base64.
 * Used in journal entry line requests.
 */
public record DocumentUploadRequest(
    /**
     * Original filename.
     */
    @NotBlank(message = "Filename is required")
    @Size(max = 255, message = "Filename must not exceed 255 characters")
    String name,

    /**
     * MIME type (e.g., application/pdf, image/jpeg).
     */
    @NotBlank(message = "MIME type is required")
    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    String type,

    /**
     * File size in bytes.
     */
    @NotNull(message = "File size is required")
    Long size,

    /**
     * File content encoded in base64.
     */
    @NotBlank(message = "File content (base64) is required")
    String base64
) {
    /**
     * Compact constructor for normalization.
     */
    public DocumentUploadRequest {
        if (name != null) {
            name = name.trim();
        }
        if (type != null) {
            type = type.trim();
        }
    }
}
