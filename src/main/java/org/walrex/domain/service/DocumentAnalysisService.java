package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.walrex.application.port.input.AnalyzeDocumentUseCase;
import org.walrex.application.port.output.DocumentIntelligencePort;
import org.walrex.domain.exception.DocumentTooLargeException;
import org.walrex.domain.exception.DocumentUnreadableException;
import org.walrex.domain.exception.UnsupportedDocumentFormatException;
import org.walrex.domain.model.DocumentAnalysisResult;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;
import org.walrex.infrastructure.adapter.outbound.filesystem.ReactiveFileSystemService;

import java.nio.file.Path;
import java.util.Set;

/**
 * Servicio de dominio que implementa el caso de uso de análisis de documentos.
 * Orquesta la validación y el procesamiento de documentos mediante Azure Document Intelligence.
 * FULLY REACTIVE - No blocking I/O operations
 */
@Slf4j
@ApplicationScoped
public class DocumentAnalysisService implements AnalyzeDocumentUseCase {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/octet-stream"
    );

    @Inject
    DocumentIntelligencePort documentIntelligencePort;

    @Inject
    ReactiveFileSystemService fileSystemService;

    @Override
    @WithSpan("DocumentAnalysisService.analyzeDocument")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<DocumentAnalysisResult> analyzeDocument(FileUpload fileUpload) {
        log.info("Analyzing document: {}", fileUpload.fileName());

        String fileName = fileUpload.fileName();
        String contentType = fileUpload.contentType();
        Path filePath = fileUpload.filePath();

        return fileSystemService.readFile(filePath)
                .chain(documentBytes -> {
                    validateFileSize(documentBytes.length, fileName);
                    validateContentType(contentType, fileName);
                    return documentIntelligencePort.analyzeInvoice(documentBytes, contentType);
                })
                .onFailure().transform(throwable -> {
                    log.error("Error analyzing document: {}", fileName, throwable);
                    return new DocumentUnreadableException(
                            "Error al analizar el documento: " + fileName,
                            throwable
                    );
                });
    }

    @Override
    @WithSpan("DocumentAnalysisService.analyzeDocumentBytes")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<DocumentAnalysisResult> analyzeDocument(byte[] documentBytes, String contentType, String fileName) {
        log.info("Analyzing document from bytes: {} (type: {})", fileName, contentType);

        // Validar tamaño
        validateFileSize(documentBytes.length, fileName);

        // Validar formato
        validateContentType(contentType, fileName);

        // Delegar al puerto de salida
        return documentIntelligencePort.analyzeInvoice(documentBytes, contentType)
                .onFailure().transform(throwable -> {
                    log.error("Error analyzing document: {}", fileName, throwable);
                    return new DocumentUnreadableException(
                            "Error al analizar el documento: " + fileName,
                            throwable
                    );
                });
    }

    /**
     * Valida que el tamaño del archivo no exceda el límite permitido.
     */
    private void validateFileSize(long fileSize, String fileName) {
        if (fileSize > MAX_FILE_SIZE) {
            log.warn("File size exceeds limit: {} bytes (max: {})", fileSize, MAX_FILE_SIZE);
            throw new DocumentTooLargeException(fileSize, MAX_FILE_SIZE);
        }
        log.debug("File size validation passed for {}: {} bytes", fileName, fileSize);
    }

    /**
     * Valida que el tipo de contenido esté soportado.
     */
    private void validateContentType(String contentType, String fileName) {
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Unsupported content type for file {}: {}", fileName, contentType);
            throw new UnsupportedDocumentFormatException(contentType);
        }
        log.debug("Content type validation passed for {}: {}", fileName, contentType);
    }
}
