-- Migration to add management columns to journal_entries table
-- These columns enable soft delete, audit tracking, and status management

-- Add updated_at column for tracking last modification timestamp
ALTER TABLE public.journal_entries
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NULL;

-- Add deleted_at column for soft delete functionality
ALTER TABLE public.journal_entries
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Create enum type for entry status if it doesn't exist
DO $$ BEGIN
    CREATE TYPE entry_status AS ENUM ('ACTIVE', 'VOIDED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Add status column with default value 'ACTIVE'
ALTER TABLE public.journal_entries
ADD COLUMN IF NOT EXISTS status entry_status DEFAULT 'ACTIVE'::entry_status NOT NULL;

-- Add comment to the columns
COMMENT ON COLUMN public.journal_entries.updated_at IS 'Timestamp of last update';
COMMENT ON COLUMN public.journal_entries.deleted_at IS 'Timestamp of soft delete (NULL if not deleted)';
COMMENT ON COLUMN public.journal_entries.status IS 'Entry status: ACTIVE or VOIDED';

-- Create index on deleted_at for efficient queries filtering deleted entries
CREATE INDEX IF NOT EXISTS idx_journal_entries_deleted_at
ON public.journal_entries(deleted_at) WHERE deleted_at IS NULL;

-- Create index on status for efficient status-based queries
CREATE INDEX IF NOT EXISTS idx_journal_entries_status
ON public.journal_entries(status);

-- Create index on entry_date for efficient date range queries
CREATE INDEX IF NOT EXISTS idx_journal_entries_entry_date
ON public.journal_entries(entry_date);

-- Create index on book_type for efficient filtering by book type
CREATE INDEX IF NOT EXISTS idx_journal_entries_book_type
ON public.journal_entries(book_type);

-- Create composite index for common query pattern (book_type + entry_date)
CREATE INDEX IF NOT EXISTS idx_journal_entries_book_type_entry_date
ON public.journal_entries(book_type, entry_date);
