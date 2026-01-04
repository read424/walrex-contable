package org.walrex.domain.exception;

/**
 * Excepción lanzada cuando el documento excede el tamaño máximo permitido.
 */
public class DocumentTooLargeException extends DocumentAnalysisException {

    private final long fileSize;
    private final long maxSize;

    public DocumentTooLargeException(long fileSize, long maxSize) {
        super(String.format("El documento excede el tamaño máximo permitido. Tamaño: %d bytes, Máximo: %d bytes",
                fileSize, maxSize));
        this.fileSize = fileSize;
        this.maxSize = maxSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getMaxSize() {
        return maxSize;
    }
}
