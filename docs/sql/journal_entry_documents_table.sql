-- Table for storing journal entry line document metadata
-- Files are stored in the filesystem, this table stores only metadata and paths

CREATE TABLE IF NOT EXISTS public.journal_entry_documents (
    id SERIAL PRIMARY KEY,
    journal_entry_line_id INTEGER NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT NOW() NOT NULL,

    CONSTRAINT fk_journal_entry_line
        FOREIGN KEY (journal_entry_line_id)
        REFERENCES public.journal_entry_lines(id)
        ON DELETE CASCADE
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_journal_entry_documents_line_id
    ON public.journal_entry_documents(journal_entry_line_id);

CREATE INDEX IF NOT EXISTS idx_journal_entry_documents_uploaded_at
    ON public.journal_entry_documents(uploaded_at);

-- Comments
COMMENT ON TABLE public.journal_entry_documents IS 'Stores metadata for documents attached to journal entry lines';
COMMENT ON COLUMN public.journal_entry_documents.original_filename IS 'Original filename as uploaded by user';
COMMENT ON COLUMN public.journal_entry_documents.stored_filename IS 'Unique filename stored in filesystem (UUID-based)';
COMMENT ON COLUMN public.journal_entry_documents.file_path IS 'Full path to file in filesystem';
COMMENT ON COLUMN public.journal_entry_documents.mime_type IS 'MIME type of the file (e.g., application/pdf, image/jpeg)';
COMMENT ON COLUMN public.journal_entry_documents.file_size IS 'File size in bytes';
