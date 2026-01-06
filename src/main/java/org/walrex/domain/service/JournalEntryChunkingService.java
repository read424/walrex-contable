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
    public Uni<HistoricalEntryChunk> createHistoricalChunk(JournalEntry entry) {
        log.info("Creating historical chunk for journal entry ID: {}", entry.getId());

        String chunkText = createChunk(entry);

        return embeddingService.generate(chunkText)
                .map(embedding -> HistoricalEntryChunk.builder()
                        .journalEntryId(entry.getId())
                        .entryDate(entry.getEntryDate())
                        .description(entry.getDescription())
                        .bookType(entry.getBookType())
                        .chunkText(chunkText)
                        .embedding(embedding)
                        .totalDebit(entry.getTotalDebit())
                        .totalCredit(entry.getTotalCredit())
                        .build()
                );
    }
}
