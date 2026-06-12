package org.walrex.infrastructure.adapter.outbound.filesystem;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.output.FileStoragePort;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementation of FileStoragePort that stores files in the local filesystem.
 * Files are organized by date: /uploads/journal-entries/YYYY/MM/DD/
 * FULLY REACTIVE - No blocking I/O operations
 */
@Slf4j
@ApplicationScoped
public class FileStorageAdapter implements FileStoragePort {

    @ConfigProperty(name = "file.storage.base-path", defaultValue = "./uploads")
    String basePath;

    @Inject
    ReactiveFileSystemService fileSystemService;

    private static final String JOURNAL_ENTRY_FOLDER = "journal-entries";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public Uni<StoredFileInfo> store(InputStream inputStream, String originalFilename, String mimeType, Long fileSize) {
        return Uni.createFrom().item(() -> {
            String extension = getFileExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

            LocalDate now = LocalDate.now();
            String datePath = now.format(DATE_FORMATTER);
            Path directory = Paths.get(basePath, JOURNAL_ENTRY_FOLDER, datePath);
            Path filePath = directory.resolve(storedFilename);

            return new Object[]{filePath, directory, storedFilename};
        })
        .flatMap(data -> {
            Path filePath = (Path) ((Object[]) data)[0];
            Path directory = (Path) ((Object[]) data)[1];
            String storedFilename = (String) ((Object[]) data)[2];

            return fileSystemService.createDirectories(directory)
                .flatMap(v -> fileSystemService.copyFromInputStream(inputStream, filePath))
                .invoke(() -> log.info("File stored successfully: {} -> {}", originalFilename, filePath))
                .map(bytesWritten -> new StoredFileInfo(storedFilename, filePath.toString()))
                .onFailure().invoke(error -> log.error("Error storing file: {}", originalFilename, error));
        });
    }

    @Override
    public Uni<Boolean> delete(String filePath) {
        Path path = Paths.get(filePath);
        return fileSystemService.deleteIfExists(path)
                .invoke(deleted -> {
                    if (deleted) {
                        log.info("File deleted successfully: {}", filePath);
                    } else {
                        log.warn("File not found for deletion: {}", filePath);
                    }
                })
                .onFailure().invoke(error -> log.error("Error deleting file: {}", filePath, error))
                .onFailure().recoverWithItem(false);
    }

    @Override
    public Uni<Boolean> exists(String filePath) {
        Path path = Paths.get(filePath);
        return fileSystemService.exists(path)
                .onFailure().recoverWithItem(false);
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
