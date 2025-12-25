package org.walrex.domain.model;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Domain model representing a document attached to a journal entry line.
 * Files are stored in the filesystem, this model contains only metadata.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class JournalEntryDocument {

    /**
     * Unique identifier for the document.
     */
    private Integer id;

    /**
     * Foreign key to the journal entry line.
     */
    private Integer journalEntryLineId;

    /**
     * Original filename as uploaded by user.
     */
    private String originalFilename;

    /**
     * Unique filename stored in filesystem (UUID-based to avoid conflicts).
     */
    private String storedFilename;

    /**
     * Full path to file in filesystem.
     */
    private String filePath;

    /**
     * MIME type of the file (e.g., application/pdf, image/jpeg).
     */
    private String mimeType;

    /**
     * File size in bytes.
     */
    private Long fileSize;

    /**
     * Timestamp when the file was uploaded.
     */
    private OffsetDateTime uploadedAt;

    /**
     * Checks if the document is a PDF.
     *
     * @return true if MIME type is application/pdf
     */
    public boolean isPdf() {
        return "application/pdf".equalsIgnoreCase(mimeType);
    }

    /**
     * Checks if the document is an image.
     *
     * @return true if MIME type starts with image/
     */
    public boolean isImage() {
        return mimeType != null && mimeType.toLowerCase().startsWith("image/");
    }

    /**
     * Gets the file extension from the original filename.
     *
     * @return file extension (e.g., "pdf", "jpg") or empty string if none
     */
    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
    }
}
