-- Agregar campos de screening, KYC extendido y nacionalidad a la tabla clients
ALTER TABLE public.clients
    ADD COLUMN IF NOT EXISTS screening_decision     varchar(10)  DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS screening_score        decimal(8,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS screening_datasets     varchar(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS screening_entity_id    varchar(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS screening_last_checked timestamptz  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS kyc_reviewed_by        int4         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS kyc_reviewed_at        timestamptz  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS kyc_approved_at        timestamptz  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS kyc_expires_at         date         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS kyc_notes              text         DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS nationality            varchar(3)   DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS id_country_birth       int4         DEFAULT NULL;

-- Historial de screening: cada ejecución queda registrada
CREATE TABLE IF NOT EXISTS public.client_screening_history (
    id                 serial4      NOT NULL,
    client_id          int4         NOT NULL,
    decision           varchar(10)  NOT NULL,
    score              decimal(8,4) NOT NULL,
    datasets           varchar(100) DEFAULT NULL,
    entity_id          varchar(100) DEFAULT NULL,
    identifier_matched boolean      DEFAULT false,
    triggered_by       varchar(20)  DEFAULT 'AUTO',
    checked_at         timestamptz  DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT client_screening_history_pk PRIMARY KEY (id),
    CONSTRAINT fk_screening_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE INDEX idx_screening_history_client ON public.client_screening_history(client_id);
