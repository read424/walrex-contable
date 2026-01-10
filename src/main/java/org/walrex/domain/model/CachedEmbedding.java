package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedEmbedding {
    /**
     * Vector embedding (1536 dimensiones para text-embedding-3-small)
     */
    private float[] embedding;

    /**
     * Texto del chunk semántico que se embeddeó
     */
    private String chunkText;

    /**
     * Timestamp de cuándo se generó
     */
    private Instant timestamp;
}
