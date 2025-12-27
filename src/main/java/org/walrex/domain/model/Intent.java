package org.walrex.domain.model;

import java.util.List;

/**
 * Representa una intención detectada del usuario
 */
public record Intent(
        String intentName,
        String description,
        List<String> examplePhrases,
        String toolName,
        String promptTemplate,
        double similarityScore
) {}
