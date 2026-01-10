package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.AccountingAccountQueryPort;
import org.walrex.domain.model.HistoricalEntryChunk;
import org.walrex.domain.model.JournalEntry;
import org.walrex.domain.model.JournalEntryLine;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para convertir asientos contables en chunks para Qdrant.
 */
@Slf4j
@ApplicationScoped
public class JournalEntryChunkingService {

    @Inject
    EmbeddingGeneratorService embeddingService;

    @Inject
    AccountingAccountQueryPort accountQueryPort;

    /**
     * Crea un chunk de texto formateado a partir de un asiento contable.
     */
    public String createChunk(JournalEntry entry) {
        StringBuilder chunk = new StringBuilder();

        chunk.append(String.format("Fecha: %s. ", entry.getEntryDate()));
        chunk.append(String.format("Tipo: %s. ", entry.getBookType()));
        chunk.append(String.format("Descripción: %s. ", entry.getDescription()));
        chunk.append("Líneas: ");

        for (JournalEntryLine line : entry.getLines()) {
            // Determinar si es débito o crédito
            String amount;
            if (line.getDebit() != null && line.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                amount = "Débito " + line.getDebit();
            } else {
                amount = "Crédito " + line.getCredit();
            }

            chunk.append(String.format("[Línea: %s, %s] ",
                    line.getDescription() != null ? line.getDescription() : "Sin descripción",
                    amount));
        }

        return chunk.toString();
    }

    /**
     * Crea un HistoricalEntryChunk completo con embedding.
     */
    public Uni<HistoricalEntryChunk> createHistoricalChunk(JournalEntry entry, String conceptHash) {
        log.info("Creating historical chunk for journal entry ID: {}", entry.getId());

        String chunkText = createChunk(entry);

        log.info("Created chunkText {}", chunkText);

        return embeddingService.generate(chunkText)
                .chain(embedding -> buildHistoricalChunk(entry, chunkText, embedding, conceptHash));
    }

    /**
     * Crea un HistoricalEntryChunk REUTILIZANDO el embedding del cache.
     * 🔑 MÉTODO CLAVE para ahorro de costos y latencia.
     *
     * @param entry Asiento contable guardado
     * @param cachedEmbedding Embedding recuperado del cache (ya generado previamente)
     * @param conceptHash Hash SHA-256 de la imagen original
     * @return HistoricalEntryChunk listo para almacenar en Qdrant
     */
    public Uni<HistoricalEntryChunk> createHistoricalChunkFromCache(
            JournalEntry entry,
            float[] cachedEmbedding,
            String conceptHash) {

        log.info("Creating historical chunk from CACHED embedding for journal entry ID: {}", entry.getId());

        String chunkText = createChunk(entry);

        return buildHistoricalChunk(entry, chunkText, cachedEmbedding, conceptHash);
    }

    /**
     * Método auxiliar para construir HistoricalEntryChunk.
     * Obtiene los códigos de cuenta de forma reactiva desde el repositorio.
     */
    private Uni<HistoricalEntryChunk> buildHistoricalChunk(
            JournalEntry entry,
            String chunkText,
            float[] embedding,
            String conceptHash) {

        // Extraer IDs de cuentas únicos
        List<Integer> accountIds = entry.getLines().stream()
                .map(JournalEntryLine::getAccountId)
                .distinct()
                .toList();

        // Obtener códigos de cuenta de forma reactiva
        @SuppressWarnings("unchecked")
        Uni<String>[] accountCodeUnis = accountIds.stream()
                .map(accountId -> accountQueryPort.findById(accountId)
                        .map(optAccount -> optAccount
                                .map(account -> account.getCode())
                                .orElse("UNKNOWN")
                        )
                )
                .toArray(Uni[]::new);

        // Combinar todos los Unis en paralelo
        return Uni.combine().all().unis(accountCodeUnis)
                .combinedWith(codes -> {
                    String accountCodes = ((List<String>) codes).stream()
                            .collect(Collectors.joining(","));

                    return HistoricalEntryChunk.builder()
                            .journalEntryId(entry.getId())
                            .entryDate(entry.getEntryDate())
                            .description(entry.getDescription())
                            .bookType(entry.getBookType())
                            .chunkText(chunkText)
                            .embedding(embedding)
                            .totalDebit(entry.getTotalDebit())
                            .totalCredit(entry.getTotalCredit())
                            .conceptHash(conceptHash)      // SHA-256 de la imagen original
                            .accountCodes(accountCodes)     // "16111,101011"
                            .build();
                });
    }
}
