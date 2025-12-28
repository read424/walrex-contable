package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * JPA entity for journal entry line documents.
 * Files are stored in the filesystem, this entity stores only metadata.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString(exclude = "journalEntryLine")
@EqualsAndHashCode(callSuper = true, exclude = "journalEntryLine")
@Entity
@Table(name = "journal_entry_documents")
public class JournalEntryDocumentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "journal_entry_line_id", nullable = false)
    private JournalEntryLineEntity journalEntryLine;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;
}
