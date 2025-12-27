package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.walrex.infrastructure.adapter.outbound.persistence.type.VectorType;

import java.time.LocalDateTime;

/**
 * Entidad que representa un intent (intención) del usuario con su embedding
 * para búsqueda semántica usando pgvector
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "intent_embeddings", uniqueConstraints = {
        @UniqueConstraint(name = "intent_name_unique", columnNames = {"intent_name"})
})
public class IntentEmbeddingEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intent_name", nullable = false, length = 100)
    private String intentName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "example_phrases", columnDefinition = "TEXT[]")
    private String[] examplePhrases;

    @Type(VectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
