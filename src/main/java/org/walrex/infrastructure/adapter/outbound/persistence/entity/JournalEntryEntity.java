package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.EntryStatus;
import org.walrex.infrastructure.adapter.outbound.listener.JournalEntryEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "journal_entries")
// @EntityListeners(JournalEntryEntityListener.class) // DISABLED: No funciona correctamente con Hibernate Reactive
// La sincronización a Qdrant se hace manualmente desde JournalEntryHandler
public class JournalEntryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "operation_number")
    private Integer operationNumber;

    @Column(name = "book_correlative")
    private Integer bookCorrelative;

    @Column(name = "book_type", nullable = false)
    private AccountingBookType bookType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EntryStatus status;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<JournalEntryLineEntity> lines = new ArrayList<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Helper method to add a line to the journal entry.
     * Maintains bidirectional relationship.
     */
    public void addLine(JournalEntryLineEntity line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    /**
     * Helper method to remove a line from the journal entry.
     */
    public void removeLine(JournalEntryLineEntity line) {
        lines.remove(line);
        line.setJournalEntry(null);
    }

    /**
     * Helper method to clear all lines.
     */
    public void clearLines() {
        lines.forEach(line -> line.setJournalEntry(null));
        lines.clear();
    }
}
