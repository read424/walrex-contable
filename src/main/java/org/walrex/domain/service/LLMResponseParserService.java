package org.walrex.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para parsear respuestas JSON del LLM a modelos de dominio.
 */
@Slf4j
@ApplicationScoped
public class LLMResponseParserService {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Parsea la respuesta JSON del LLM a JournalEntrySuggestion.
     */
    public Uni<JournalEntrySuggestion> parseToJournalEntrySuggestion(
            String llmResponse,
            RAGContext context,
            RetrievedContext retrievedContext,
            String llmProvider) {

        return Uni.createFrom().item(() -> {
            try {
                // Extraer JSON de la respuesta (puede venir con markdown)
                String jsonString = extractJSON(llmResponse);
                JsonNode root = objectMapper.readTree(jsonString);

                // Parsear líneas
                List<JournalEntryLine> lines = parseLines(root.get("lines"));

                // Calcular totales
                BigDecimal totalDebit = lines.stream()
                        .map(line -> line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCredit = lines.stream()
                        .map(line -> line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return JournalEntrySuggestion.builder()
                        .suggestedDate(context.getEntryDate())
                        .suggestedDescription(root.get("description").asText())
                        .suggestedBookType(context.getBookType())
                        .suggestedLines(lines)
                        .totalDebit(totalDebit)
                        .totalCredit(totalCredit)
                        .isBalanced(totalDebit.compareTo(totalCredit) == 0)
                        .retrievedContext(retrievedContext)
                        .llmExplanation(root.get("explanation").asText())
                        .llmProviderUsed(llmProvider)
                        .overallConfidence(root.get("confidence").floatValue())
                        .build();

            } catch (Exception e) {
                log.error("Failed to parse LLM response", e);
                throw new RuntimeException("Failed to parse LLM response: " + e.getMessage());
            }
        });
    }

    /**
     * Extrae JSON si viene con markdown ```json ... ```
     */
    private String extractJSON(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.lastIndexOf("```");
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.lastIndexOf("```");
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        return response.trim();
    }

    /**
     * Parsea las líneas del JSON a objetos JournalEntryLine.
     */
    private List<JournalEntryLine> parseLines(JsonNode linesNode) {
        List<JournalEntryLine> lines = new ArrayList<>();

        if (linesNode == null || !linesNode.isArray()) {
            return lines;
        }

        for (JsonNode lineNode : linesNode) {
            try {
                Integer accountId = lineNode.has("accountId") ?
                        lineNode.get("accountId").asInt() : null;

                BigDecimal debit = lineNode.has("debit") ?
                        new BigDecimal(lineNode.get("debit").asText()) : BigDecimal.ZERO;

                BigDecimal credit = lineNode.has("credit") ?
                        new BigDecimal(lineNode.get("credit").asText()) : BigDecimal.ZERO;

                String description = lineNode.has("description") ?
                        lineNode.get("description").asText() : null;

                JournalEntryLine line = JournalEntryLine.builder()
                        .accountId(accountId)
                        .debit(debit)
                        .credit(credit)
                        .description(description)
                        .build();

                lines.add(line);

            } catch (Exception e) {
                log.warn("Failed to parse line, skipping: {}", lineNode, e);
            }
        }

        return lines;
    }
}
