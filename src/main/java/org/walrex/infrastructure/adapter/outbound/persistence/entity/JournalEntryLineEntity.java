package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString(exclude = "journalEntry")
@EqualsAndHashCode(callSuper = true, exclude = "journalEntry")
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
}
