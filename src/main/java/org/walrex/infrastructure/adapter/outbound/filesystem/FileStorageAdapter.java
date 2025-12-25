package org.walrex.infrastructure.adapter.outbound.filesystem;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.output.FileStoragePort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementation of FileStoragePort that stores files in the local filesystem.
 * Files are organized by date: /uploads/journal-entries/YYYY/MM/DD/
 */
@Slf4j
@ApplicationScoped
public class FileStorageAdapter implements FileStoragePort {

    @ConfigProperty(name = "file.storage.base-path", defaultValue = "./uploads")
    String basePath;

    private static final String JOURNAL_ENTRY_FOLDER = "journal-entries";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public Uni<StoredFileInfo> store(InputStream inputStream, String originalFilename, String mimeType, Long fileSize) {
        return Uni.createFrom().item(() -> {
            try {
                // Generate unique filename using UUID
                String extension = getFileExtension(originalFilename);
                String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

                // Create date-based directory structure
                LocalDate now = LocalDate.now();
                String datePath = now.format(DATE_FORMATTER);
                Path directory = Paths.get(basePath, JOURNAL_ENTRY_FOLDER, datePath);

                // Create directories if they don't exist
                Files.createDirectories(directory);

                // Full path to store the file
                Path filePath = directory.resolve(storedFilename);

                // Copy file to destination
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

                log.info("File stored successfully: {} -> {}", originalFilename, filePath);

                return new StoredFileInfo(storedFilename, filePath.toString());
            } catch (IOException e) {
                log.error("Error storing file: {}", originalFilename, e);
                throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public Uni<Boolean> delete(String filePath) {
        return Uni.createFrom().item(() -> {
            try {
                Path path = Paths.get(filePath);
                boolean deleted = Files.deleteIfExists(path);

                if (deleted) {
                    log.info("File deleted successfully: {}", filePath);
                } else {
                    log.warn("File not found for deletion: {}", filePath);
                }

                return deleted;
            } catch (IOException e) {
                log.error("Error deleting file: {}", filePath, e);
                return false;
            }
        });
    }

    @Override
    public Uni<Boolean> exists(String filePath) {
        return Uni.createFrom().item(() -> {
            Path path = Paths.get(filePath);
            return Files.exists(path);
        });
    }

    @Override
    public Path getFullPath(String storedFilename) {
        // For simplicity, search in today's folder first
        // In production, you might want to store the date with the filename or search recursively
        LocalDate now = LocalDate.now();
        String datePath = now.format(DATE_FORMATTER);
        return Paths.get(basePath, JOURNAL_ENTRY_FOLDER, datePath, storedFilename);
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename The filename
     * @return Extension without dot (e.g., "pdf", "jpg") or empty string
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
