package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString(exclude = {"journalEntry", "documents"})
@EqualsAndHashCode(callSuper = true, exclude = {"journalEntry", "documents"})
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLineEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryEntity journalEntry;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal debit;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal credit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "journalEntryLine", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JournalEntryDocumentEntity> documents = new ArrayList<>();

    /**
     * Helper method to add a document to the line.
     * Maintains bidirectional relationship.
     */
    public void addDocument(JournalEntryDocumentEntity document) {
        documents.add(document);
        document.setJournalEntryLine(this);
    }

    /**
     * Helper method to remove a document from the line.
     */
    public void removeDocument(JournalEntryDocumentEntity document) {
        documents.remove(document);
        document.setJournalEntryLine(null);
    }
}
