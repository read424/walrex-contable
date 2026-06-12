package org.walrex.infrastructure.adapter.outbound.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.infrastructure.adapter.outbound.filesystem.ReactiveFileSystemService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Async logger especializado para debug de generación de embeddings.
 * Escribe logs estructurados en JSON sin bloquear el event loop.
 * Reemplaza al anterior EmbeddingDebugLogger que tenía synchronized bloqueante.
 */
@Slf4j
@ApplicationScoped
public class AsyncFileLogger {

    private static final String LOG_DIR = "logs/embeddings";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final ObjectMapper objectMapper;
    private final Path currentLogFile;

    @Inject
    ReactiveFileSystemService fileSystemService;

    public AsyncFileLogger() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        try {
            Path logDir = Paths.get(LOG_DIR);
            Files.createDirectories(logDir);
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            this.currentLogFile = logDir.resolve("embedding-generation-" + timestamp + ".json");
            log.info("📝 Debug logs will be written to: {}", currentLogFile.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize debug logger", e);
        }
    }

    /**
     * Log evento de forma asincrónica sin bloquear
     */
    public Uni<Void> logEvent(String intentName, String phase, String status, Map<String, Object> data) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().toString());
            logEntry.put("intentName", intentName);
            logEntry.put("phase", phase);
            logEntry.put("status", status);
            logEntry.put("threadName", Thread.currentThread().getName());
            if (data != null) {
                logEntry.putAll(data);
            }
            return objectMapper.writeValueAsString(logEntry) + ",\n";
        }))
        .flatMap(json -> fileSystemService.appendFile(currentLogFile, json))
        .onFailure().invoke(error -> log.error("Failed to write debug log", error));
    }

    public Uni<Void> logStart(String intentName, int phrasesCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("examplePhrasesCount", phrasesCount);
        return logEvent(intentName, "START", "INFO", data);
    }

    public Uni<Void> logEmbeddingGenerated(String intentName, String phrase, int dimensions) {
        Map<String, Object> data = new HashMap<>();
        data.put("phrase", phrase.substring(0, Math.min(50, phrase.length())) + "...");
        data.put("embeddingDimensions", dimensions);
        return logEvent(intentName, "EMBEDDING_GENERATED", "SUCCESS", data);
    }

    public Uni<Void> logAveraged(String intentName, int embeddingsCount, int dimensions) {
        Map<String, Object> data = new HashMap<>();
        data.put("embeddingsCount", embeddingsCount);
        data.put("averagedDimensions", dimensions);
        return logEvent(intentName, "AVERAGING", "SUCCESS", data);
    }

    public Uni<Void> logThreadSwitch(String intentName, String fromThread, String toThread) {
        Map<String, Object> data = new HashMap<>();
        data.put("fromThread", fromThread);
        data.put("toThread", toThread);
        return logEvent(intentName, "THREAD_SWITCH", "INFO", data);
    }

    public Uni<Void> logTransactionStart(String intentName) {
        return logEvent(intentName, "TRANSACTION_START", "INFO", null);
    }

    public Uni<Void> logEntityFound(String intentName, Long entityId, boolean hasEmbedding) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityId", entityId);
        data.put("hadEmbeddingBefore", hasEmbedding);
        return logEvent(intentName, "ENTITY_LOADED", "SUCCESS", data);
    }

    public Uni<Void> logEmbeddingSet(String intentName, int dimensions) {
        Map<String, Object> data = new HashMap<>();
        data.put("newEmbeddingDimensions", dimensions);
        return logEvent(intentName, "EMBEDDING_SET", "SUCCESS", data);
    }

    public Uni<Void> logTransactionCommit(String intentName) {
        return logEvent(intentName, "TRANSACTION_COMMIT", "SUCCESS", null);
    }

    public Uni<Void> logError(String intentName, String phase, Throwable error) {
        Map<String, Object> data = new HashMap<>();
        data.put("errorClass", error.getClass().getName());
        data.put("errorMessage", error.getMessage());

        StackTraceElement[] stackTrace = error.getStackTrace();
        String[] topStack = new String[Math.min(5, stackTrace.length)];
        for (int i = 0; i < topStack.length; i++) {
            topStack[i] = stackTrace[i].toString();
        }
        data.put("stackTraceTop", topStack);

        if (error.getCause() != null) {
            data.put("causeClass", error.getCause().getClass().getName());
            data.put("causeMessage", error.getCause().getMessage());
        }

        return logEvent(intentName, phase, "ERROR", data);
    }

    public Uni<Void> logComplete(String intentName, boolean success) {
        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        return logEvent(intentName, "COMPLETE", success ? "SUCCESS" : "FAILED", data);
    }

    public Path getLogFilePath() {
        return currentLogFile;
    }
}
