package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.walrex.domain.model.AccountingBookType;
import org.walrex.domain.model.EntryStatus;

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
public class JournalEntryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String reference;

    @Column(name = "doc_type_id")
    private Integer docTypeId;

    @Column(name = "doc_serie", length = 10)
    private String docSerie;

    @Column(name = "doc_number", length = 20)
    private String docNumber;

    @Column(name = "operation_number")
    private Integer operationNumber;

    @Column(name = "book_correlative")
    private Integer bookCorrelative;

    @Column(name = "book_type", nullable = false)
    private AccountingBookType bookType;

    @Column(length = 20)
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
