package org.walrex.infrastructure.adapter.outbound.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Logger especializado para debug de generación de embeddings
 * Escribe logs estructurados en JSON para análisis sin contaminar con arrays gigantes
 */
@Slf4j
@ApplicationScoped
public class EmbeddingDebugLogger {

    private static final String LOG_DIR = "logs/embeddings";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final ObjectMapper objectMapper;
    private final Path currentLogFile;

    public EmbeddingDebugLogger() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Crear directorio de logs si no existe
            Path logDir = Paths.get(LOG_DIR);
            Files.createDirectories(logDir);

            // Archivo de log para esta sesión
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            this.currentLogFile = logDir.resolve("embedding-generation-" + timestamp + ".json");

            log.info("📝 Debug logs will be written to: {}", currentLogFile.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize debug logger", e);
        }
    }

    public void logEvent(String intentName, String phase, String status, Map<String, Object> data) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().toString());
        logEntry.put("intentName", intentName);
        logEntry.put("phase", phase);
        logEntry.put("status", status);
        logEntry.put("threadName", Thread.currentThread().getName());

        if (data != null) {
            logEntry.putAll(data);
        }

        writeToFile(logEntry);
    }

    public void logStart(String intentName, int phrasesCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("examplePhrasesCount", phrasesCount);
        logEvent(intentName, "START", "INFO", data);
    }

    public void logEmbeddingGenerated(String intentName, String phrase, int dimensions) {
        Map<String, Object> data = new HashMap<>();
        data.put("phrase", phrase.substring(0, Math.min(50, phrase.length())) + "...");
        data.put("embeddingDimensions", dimensions);
        logEvent(intentName, "EMBEDDING_GENERATED", "SUCCESS", data);
    }

    public void logAveraged(String intentName, int embeddingsCount, int dimensions) {
        Map<String, Object> data = new HashMap<>();
        data.put("embeddingsCount", embeddingsCount);
        data.put("averagedDimensions", dimensions);
        logEvent(intentName, "AVERAGING", "SUCCESS", data);
    }

    public void logThreadSwitch(String intentName, String fromThread, String toThread) {
        Map<String, Object> data = new HashMap<>();
        data.put("fromThread", fromThread);
        data.put("toThread", toThread);
        logEvent(intentName, "THREAD_SWITCH", "INFO", data);
    }

    public void logTransactionStart(String intentName) {
        logEvent(intentName, "TRANSACTION_START", "INFO", null);
    }

    public void logEntityFound(String intentName, Long entityId, boolean hasEmbedding) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityId", entityId);
        data.put("hadEmbeddingBefore", hasEmbedding);
        logEvent(intentName, "ENTITY_LOADED", "SUCCESS", data);
    }

    public void logEmbeddingSet(String intentName, int dimensions) {
        Map<String, Object> data = new HashMap<>();
        data.put("newEmbeddingDimensions", dimensions);
        logEvent(intentName, "EMBEDDING_SET", "SUCCESS", data);
    }

    public void logTransactionCommit(String intentName) {
        logEvent(intentName, "TRANSACTION_COMMIT", "SUCCESS", null);
    }

    public void logError(String intentName, String phase, Throwable error) {
        Map<String, Object> data = new HashMap<>();
        data.put("errorClass", error.getClass().getName());
        data.put("errorMessage", error.getMessage());

        // Stack trace (primeras 5 líneas)
        StackTraceElement[] stackTrace = error.getStackTrace();
        String[] topStack = new String[Math.min(5, stackTrace.length)];
        for (int i = 0; i < topStack.length; i++) {
            topStack[i] = stackTrace[i].toString();
        }
        data.put("stackTraceTop", topStack);

        // Causa raíz si existe
        if (error.getCause() != null) {
            data.put("causeClass", error.getCause().getClass().getName());
            data.put("causeMessage", error.getCause().getMessage());
        }

        logEvent(intentName, phase, "ERROR", data);
    }

    public void logComplete(String intentName, boolean success) {
        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        logEvent(intentName, "COMPLETE", success ? "SUCCESS" : "FAILED", data);
    }

    private synchronized void writeToFile(Map<String, Object> logEntry) {
        try {
            String json = objectMapper.writeValueAsString(logEntry);
            Files.writeString(
                    currentLogFile,
                    json + ",\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.error("Failed to write debug log", e);
        }
    }

    public Path getLogFilePath() {
        return currentLogFile;
    }
}
