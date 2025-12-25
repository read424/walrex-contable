package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Port for file storage operations.
 * Abstracts the file system operations from the domain layer.
 */
public interface FileStoragePort {

    /**
     * Stores a file in the filesystem and returns the stored file information.
     *
     * @param inputStream File content as InputStream
     * @param originalFilename Original filename from upload
     * @param mimeType MIME type of the file
     * @param fileSize File size in bytes
     * @return Uni with StoredFileInfo containing path and stored filename
     */
    Uni<StoredFileInfo> store(InputStream inputStream, String originalFilename, String mimeType, Long fileSize);

    /**
     * Deletes a file from the filesystem.
     *
     * @param filePath Full path to the file to delete
     * @return Uni<Boolean> true if deleted successfully
     */
    Uni<Boolean> delete(String filePath);

    /**
     * Checks if a file exists in the filesystem.
     *
     * @param filePath Full path to check
     * @return Uni<Boolean> true if file exists
     */
    Uni<Boolean> exists(String filePath);

    /**
     * Gets the full path for a stored file.
     *
     * @param storedFilename The UUID-based stored filename
     * @return Full path to the file
     */
    Path getFullPath(String storedFilename);

    /**
     * Record containing information about a stored file.
     */
    record StoredFileInfo(
            String storedFilename,  // UUID-based unique filename
            String filePath          // Full path where file is stored
    ) {
    }
}
