-- =============================================================================
-- V1__initial_schema.sql
-- Schema completo de walrex / admon-catalogo
-- Generado desde entidades JPA — reemplaza V1-V14 anteriores
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- EXTENSIONES
-- Requiere que pgvector esté disponible en el servidor PostgreSQL.
-- Si no está instalado: sudo apt-get install postgresql-16-pgvector
-- ─────────────────────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS vector;

-- ─────────────────────────────────────────────────────────────────────────────
-- TIPOS ENUMERADOS (idempotentes)
-- ─────────────────────────────────────────────────────────────────────────────
DO $$ BEGIN CREATE TYPE account_type       AS ENUM ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE'); EXCEPTION WHEN duplicate_object THEN null; END $$;
DO $$ BEGIN CREATE TYPE normal_side        AS ENUM ('DEBIT','CREDIT');                                 EXCEPTION WHEN duplicate_object THEN null; END $$;
DO $$ BEGIN CREATE TYPE accounting_book_type AS ENUM ('DIARIO','VENTAS','COMPRAS');                    EXCEPTION WHEN duplicate_object THEN null; END $$;
DO $$ BEGIN CREATE TYPE entry_status       AS ENUM ('ACTIVE','VOIDED');                                EXCEPTION WHEN duplicate_object THEN null; END $$;
DO $$ BEGIN CREATE TYPE product_type       AS ENUM ('storable','consumable','service');                EXCEPTION WHEN duplicate_object THEN null; END $$;

-- =============================================================================
-- TABLAS SIN DEPENDENCIAS EXTERNAS
-- =============================================================================

CREATE TABLE IF NOT EXISTS country (
    id             SERIAL PRIMARY KEY,
    code_iso2      VARCHAR(2),
    code_iso3      VARCHAR(3),
    numeric_code   INTEGER,
    name_iso       VARCHAR(100),
    code_phone_iso VARCHAR(10),
    status         VARCHAR(10),
    unicode_flag   VARCHAR(10),
    created_at     TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ,
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT code_phone_uk        UNIQUE (code_phone_iso),
    CONSTRAINT country_code2_unique UNIQUE (code_iso2),
    CONSTRAINT country_code3_unique UNIQUE (code_iso3),
    CONSTRAINT country_name_unique  UNIQUE (name_iso),
    CONSTRAINT id_country_pk        UNIQUE (id)
);

CREATE TABLE IF NOT EXISTS currencies (
    id          SERIAL PRIMARY KEY,
    code_iso3   VARCHAR(3),
    numericcode INTEGER,
    name        VARCHAR(100),
    symbol      VARCHAR(10),
    status      VARCHAR(10),
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uk_currency_numeric_code    UNIQUE (numericcode),
    CONSTRAINT uk_currency_alphabetic_code UNIQUE (code_iso3),
    CONSTRAINT uk_currency_name            UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS departament (
    id_departament   SERIAL PRIMARY KEY,
    cod_departament  CHAR(2)      NOT NULL,
    name_departament VARCHAR(100) NOT NULL,
    status           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP,
    deleted_at       TIMESTAMP,
    CONSTRAINT departament_name_uk UNIQUE (name_departament),
    CONSTRAINT departament_code_uk UNIQUE (cod_departament)
);
CREATE INDEX IF NOT EXISTS idx_departament_name_lower ON departament (LOWER(name_departament));

CREATE TABLE IF NOT EXISTS product_category_uom (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uk_category_uom_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS product_attributes (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    display_type VARCHAR(20)  NOT NULL DEFAULT 'select',
    is_active    BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ,
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT uk_product_attribute_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS product_brand (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    details    TEXT,
    created_at TIMESTAMPTZ,
    CONSTRAINT product_brand_name_uk UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS product_category (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    details    TEXT,
    parent_id  INTEGER REFERENCES product_category(id),
    created_at TIMESTAMPTZ,
    CONSTRAINT product_category_name_uk UNIQUE (name, parent_id)
);

CREATE TABLE IF NOT EXISTS document_types (
    id            SERIAL PRIMARY KEY,
    sunat_code    VARCHAR(3)   NOT NULL UNIQUE,
    name_document VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS sunat_document_types (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    length      INTEGER,
    pattern     VARCHAR(50),
    active      BOOLEAN,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    CONSTRAINT sunat_doc_type_code_unique UNIQUE (code),
    CONSTRAINT sunat_doc_type_pk         UNIQUE (id)
);

CREATE TABLE IF NOT EXISTS system_document_types (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_required BOOLEAN,
    for_person  BOOLEAN,
    for_company BOOLEAN,
    priority    INTEGER,
    active      BOOLEAN,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT system_document_types_code_unique UNIQUE (code),
    CONSTRAINT system_document_types_name_unique UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS ocupaciones (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo     VARCHAR(5)   NOT NULL UNIQUE,
    nombre     VARCHAR(120) NOT NULL UNIQUE,
    status     INTEGER      DEFAULT 1,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS payout_rail (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS type_accounts_bank (
    id         SERIAL PRIMARY KEY,
    det_name   VARCHAR(50) NOT NULL UNIQUE,
    status     CHAR(1),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS type_operation (
    id            SERIAL PRIMARY KEY,
    det_name      VARCHAR(50) NOT NULL,
    requires_bank BOOLEAN,
    label_helper  VARCHAR(50),
    status        CHAR(1)
);

CREATE TABLE IF NOT EXISTS accounts (
    id               SERIAL PRIMARY KEY,
    code             VARCHAR(20)  NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    type             account_type NOT NULL,
    normal_side      normal_side  NOT NULL,
    is_active        BOOLEAN DEFAULT TRUE,
    embeddings_synced BOOLEAN DEFAULT FALSE,
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT accounts_code_key        UNIQUE (code),
    CONSTRAINT accounts_name_key        UNIQUE (name, code)
);

CREATE TABLE IF NOT EXISTS merchant_qr (
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(100) NOT NULL,
    merchant_name              VARCHAR(100),
    merchant_city              VARCHAR(100),
    mcc                        VARCHAR(4),
    currency                   VARCHAR(3),
    country_code               VARCHAR(2),
    payload_format_indicator   VARCHAR(2),
    point_of_initiation_method VARCHAR(2),
    account_info               TEXT,
    created_at                 TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS market_price_tick (
    id             BIGSERIAL PRIMARY KEY,
    provider       VARCHAR(20)   NOT NULL,
    symbol         VARCHAR(50)   NOT NULL,
    currency_base  VARCHAR(10)   NOT NULL,
    currency_quote VARCHAR(10)   NOT NULL,
    price          DECIMAL(20,8) NOT NULL,
    event_type     VARCHAR(20)   NOT NULL DEFAULT 'TICK',
    change_pct     DECIMAL(10,6),
    recorded_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mpt_symbol_time   ON market_price_tick (symbol,   recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_mpt_provider_time ON market_price_tick (provider, recorded_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- intent_embeddings  (requiere pgvector)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS intent_embeddings (
    id              BIGSERIAL PRIMARY KEY,
    intent_name     VARCHAR(100) NOT NULL,
    description     TEXT,
    example_phrases TEXT[],
    embedding       vector(1024),
    tool_name       VARCHAR(100),
    prompt_template TEXT,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT intent_name_unique UNIQUE (intent_name)
);

-- =============================================================================
-- TABLAS QUE DEPENDEN DE country
-- =============================================================================

CREATE TABLE IF NOT EXISTS type_document_id (
    id         SERIAL PRIMARY KEY,
    sigla      VARCHAR(7)  NOT NULL UNIQUE,
    det_name   VARCHAR(50) NOT NULL UNIQUE,
    status     CHAR(1)     NOT NULL DEFAULT '1',
    country_id INTEGER     NOT NULL REFERENCES country(id)
);

CREATE TABLE IF NOT EXISTS financial_institution (
    id              BIGSERIAL PRIMARY KEY,
    sigla           VARCHAR(8)  NOT NULL,
    det_name        VARCHAR(80) NOT NULL,
    id_country      INTEGER     NOT NULL REFERENCES country(id),
    status          CHAR(1)     NOT NULL DEFAULT '1',
    codigo          VARCHAR(5),
    name_pay_binance VARCHAR(50),
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS remittance_countries (
    id         SERIAL PRIMARY KEY,
    id_country INTEGER NOT NULL UNIQUE REFERENCES country(id),
    is_active  CHAR(1) NOT NULL DEFAULT '1',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS country_payout_rail (
    id            SERIAL PRIMARY KEY,
    country_id    INTEGER NOT NULL REFERENCES country(id),
    payout_rail_id INTEGER NOT NULL REFERENCES payout_rail(id),
    status        CHAR(1) DEFAULT '1'
);

-- =============================================================================
-- TABLAS QUE DEPENDEN DE country + currencies
-- =============================================================================

CREATE TABLE IF NOT EXISTS country_currencies (
    id             BIGSERIAL PRIMARY KEY,
    country_id     INTEGER NOT NULL REFERENCES country(id),
    currency_id    INTEGER NOT NULL REFERENCES currencies(id),
    is_primary     BOOLEAN,
    is_operational BOOLEAN,
    effective_date DATE,
    created_at     TIMESTAMPTZ,
    CONSTRAINT uk_country_currency UNIQUE (country_id, currency_id)
);

CREATE TABLE IF NOT EXISTS wallet_country_config (
    id          SERIAL PRIMARY KEY,
    country_id  INTEGER NOT NULL,
    currency_id INTEGER NOT NULL,
    is_default  BOOLEAN DEFAULT FALSE,
    enabled     BOOLEAN DEFAULT TRUE
);

-- =============================================================================
-- TABLAS QUE DEPENDEN DE country_currencies
-- =============================================================================

CREATE TABLE IF NOT EXISTS price_exchange (
    id                 SERIAL PRIMARY KEY,
    type_operation     CHAR(1)       NOT NULL,
    id_currency_base   BIGINT        NOT NULL REFERENCES country_currencies(id),
    id_currency_quote  BIGINT        NOT NULL REFERENCES country_currencies(id),
    amount_price       DECIMAL(13,5) NOT NULL,
    is_active          CHAR(1)       NOT NULL DEFAULT '1',
    date_exchange      DATE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMETZ
);
CREATE INDEX IF NOT EXISTS idx_price_exchange_currencies_date ON price_exchange (id_currency_base, id_currency_quote, date_exchange);
CREATE INDEX IF NOT EXISTS idx_price_exchange_active_date     ON price_exchange (is_active, date_exchange DESC);

CREATE TABLE IF NOT EXISTS exchange_rate_types (
    id               SERIAL PRIMARY KEY,
    country_id       INTEGER        NOT NULL REFERENCES country(id),
    date_rate        DATE           NOT NULL DEFAULT CURRENT_DATE,
    code_rate        VARCHAR(20)    NOT NULL,
    name_rate        VARCHAR(100)   NOT NULL,
    rate_value       DECIMAL(15,6)  NOT NULL,
    base_currency_id INTEGER        REFERENCES currencies(id),
    is_active        CHAR(1)        NOT NULL DEFAULT '1',
    display_order    INTEGER        NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exchange_rate_types_value_positive CHECK (rate_value > 0),
    CONSTRAINT chk_exchange_rate_types_active         CHECK (is_active IN ('0','1')),
    CONSTRAINT uk_exchange_rate_types_unique          UNIQUE (country_id, code_rate, date_rate)
);
CREATE INDEX IF NOT EXISTS idx_exchange_rate_types_country_active ON exchange_rate_types (country_id, is_active, date_rate DESC);
CREATE INDEX IF NOT EXISTS idx_exchange_rate_types_code           ON exchange_rate_types (code_rate);

CREATE TABLE IF NOT EXISTS remittance_routes (
    id                        SERIAL PRIMARY KEY,
    id_remittance_country     INTEGER     NOT NULL REFERENCES remittance_countries(id),
    id_country_currencies_from BIGINT     NOT NULL REFERENCES country_currencies(id),
    id_country_currencies_to   BIGINT     NOT NULL REFERENCES country_currencies(id),
    intermediary_asset        VARCHAR(10) NOT NULL DEFAULT 'USDT',
    rate_provider             VARCHAR(20) NOT NULL DEFAULT 'BINANCE',
    is_active                 CHAR(1)     NOT NULL DEFAULT '1',
    created_at                TIMESTAMP   NOT NULL,
    updated_at                TIMESTAMP   NOT NULL
);

-- =============================================================================
-- TABLAS QUE DEPENDEN DE financial_institution
-- =============================================================================

CREATE TABLE IF NOT EXISTS institution_payout_rail (
    id             BIGSERIAL PRIMARY KEY,
    bank_id        BIGINT   NOT NULL REFERENCES financial_institution(id),
    payout_rail_id INTEGER  NOT NULL REFERENCES payout_rail(id),
    status         CHAR(1)  NOT NULL DEFAULT '1',
    created_at     TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ,
    required_fields JSONB
);

-- =============================================================================
-- TABLAS QUE DEPENDEN DE country_currencies + financial_institution
-- =============================================================================

CREATE TABLE IF NOT EXISTS country_currency_payment_methods (
    id                 BIGSERIAL PRIMARY KEY,
    id_country_currency BIGINT NOT NULL REFERENCES country_currencies(id)  ON DELETE CASCADE,
    id_bank            BIGINT  NOT NULL REFERENCES financial_institution(id) ON DELETE CASCADE,
    is_active          CHAR(1) NOT NULL DEFAULT '1',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ccpm_active                  CHECK (is_active IN ('0','1')),
    CONSTRAINT uk_ccpm_country_currency_bank   UNIQUE (id_country_currency, id_bank)
);
CREATE INDEX IF NOT EXISTS idx_ccpm_country_currency ON country_currency_payment_methods (id_country_currency) WHERE is_active = '1';
CREATE INDEX IF NOT EXISTS idx_ccpm_bank             ON country_currency_payment_methods (id_bank);
CREATE INDEX IF NOT EXISTS idx_ccpm_active           ON country_currency_payment_methods (is_active);
CREATE INDEX IF NOT EXISTS idx_ccpm_lookup           ON country_currency_payment_methods (id_country_currency, is_active) INCLUDE (id_bank);

-- =============================================================================
-- TABLAS DE CLIENTES Y USUARIOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS clients (
    id                     SERIAL PRIMARY KEY,
    id_type_document       INTEGER,
    num_dni                VARCHAR(20),
    apellidos              VARCHAR(100),
    nombres                VARCHAR(100),
    sexo                   VARCHAR(1),
    det_email              VARCHAR(100) NOT NULL UNIQUE,
    date_birth             DATE,
    id_profesion           INTEGER,
    is_pep                 VARCHAR(1),
    id_country_resident    INTEGER,
    id_departamento        INTEGER,
    id_provincia           INTEGER,
    id_distrito            INTEGER,
    phonemobile            VARCHAR(20),
    phone_number           VARCHAR(20),
    id_country_phone       INTEGER,
    kyc_status             VARCHAR(20)   DEFAULT 'PENDING',
    kyc_level              INTEGER       NOT NULL DEFAULT 0,
    screening_decision     VARCHAR(10)   DEFAULT 'PENDING',
    screening_score        DECIMAL(8,4)  DEFAULT 0,
    screening_datasets     VARCHAR(100),
    screening_entity_id    VARCHAR(100),
    screening_last_checked TIMESTAMPTZ,
    kyc_reviewed_by        INTEGER,
    kyc_reviewed_at        TIMESTAMPTZ,
    kyc_approved_at        TIMESTAMPTZ,
    kyc_expires_at         DATE,
    kyc_notes              TEXT,
    nationality            VARCHAR(3),
    id_country_birth       INTEGER,
    created_at             TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ,
    deleted_at             TIMESTAMPTZ,
    CONSTRAINT email_client_unique UNIQUE (det_email),
    CONSTRAINT id_clien_pk         UNIQUE (id)
);

CREATE TABLE IF NOT EXISTS client_screening_history (
    id                 SERIAL PRIMARY KEY,
    client_id          INTEGER      NOT NULL REFERENCES clients(id),
    decision           VARCHAR(10)  NOT NULL,
    score              DECIMAL(8,4) NOT NULL,
    datasets           VARCHAR(100),
    entity_id          VARCHAR(100),
    identifier_matched BOOLEAN      DEFAULT FALSE,
    triggered_by       VARCHAR(20)  DEFAULT 'AUTO',
    checked_at         TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_screening_history_client ON client_screening_history (client_id);

CREATE TABLE IF NOT EXISTS users (
    id                   SERIAL PRIMARY KEY,
    id_client            INTEGER      NOT NULL,
    username             VARCHAR(100) NOT NULL,
    username_type        VARCHAR(50)  NOT NULL,
    pin_hash             VARCHAR(255) NOT NULL,
    pin_attempts         INTEGER      NOT NULL DEFAULT 0,
    pin_locked_until     TIMESTAMPTZ,
    status               INTEGER      DEFAULT 1,
    device_trusted       BOOLEAN      DEFAULT FALSE,
    biometric_enabled    BOOLEAN      DEFAULT FALSE,
    biometric_enrolled_at TIMESTAMPTZ,
    biometric_type       VARCHAR(50),
    mfa_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_type             VARCHAR(50),
    created_at           TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ,
    CONSTRAINT users_username_unique UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS otp (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference_id VARCHAR(64)  NOT NULL UNIQUE,
    purpose      VARCHAR(32)  NOT NULL,
    target       VARCHAR(128) NOT NULL,
    otp_hash     VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_otp_reference_purpose ON otp (reference_id, purpose);
CREATE INDEX IF NOT EXISTS idx_otp_target_active     ON otp (target, used, expires_at);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id                 SERIAL PRIMARY KEY,
    user_id            INTEGER     NOT NULL,
    device_id          INTEGER,
    refresh_token_hash VARCHAR     NOT NULL,
    expires_at         TIMESTAMPTZ NOT NULL,
    revoked_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_device_token (
    id         SERIAL PRIMARY KEY,
    user_id    INTEGER      NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    platform   VARCHAR(20)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_user_device_token_token UNIQUE (token)
);

-- =============================================================================
-- TABLAS DE WALLET
-- =============================================================================

CREATE TABLE IF NOT EXISTS account_wallet (
    id                  BIGSERIAL PRIMARY KEY,
    client_id           INTEGER       NOT NULL,
    country_id          INTEGER       NOT NULL,
    currency_id         INTEGER       NOT NULL,
    available_balance   DECIMAL(18,2) NOT NULL DEFAULT 0,
    ledger_balance      DECIMAL(18,2) NOT NULL DEFAULT 0,
    is_balance_visible  BOOLEAN       NOT NULL DEFAULT TRUE,
    bank_account_number VARCHAR(34),
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_wallet_client_currency UNIQUE (client_id, country_id, currency_id)
);

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id                    BIGSERIAL PRIMARY KEY,
    reference_id          BIGINT        NOT NULL,
    wallet_id             BIGINT        NOT NULL,
    counterparty_reference VARCHAR(50),
    amount                DECIMAL(18,2) NOT NULL,
    balance_before        DECIMAL(18,2) NOT NULL,
    balance_after         DECIMAL(18,2) NOT NULL,
    operation_type        VARCHAR(30)   NOT NULL,
    operation_direction   VARCHAR(10)   NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL
);

-- =============================================================================
-- TABLAS DE BENEFICIARIOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS beneficiary (
    id              BIGSERIAL PRIMARY KEY,
    client_id       INTEGER      NOT NULL,
    country_id      INTEGER      NOT NULL,
    first_name      VARCHAR(80)  NOT NULL,
    last_name       VARCHAR(80)  NOT NULL,
    document_type   INTEGER      NOT NULL,
    document_number VARCHAR(20),
    alias           VARCHAR(80),
    status          CHAR(1)      DEFAULT '1',
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS beneficiary_account (
    id              BIGSERIAL PRIMARY KEY,
    beneficiary_id  BIGINT      NOT NULL REFERENCES beneficiary(id),
    payout_rail_id  INTEGER     NOT NULL,
    bank_id         BIGINT,
    account_number  VARCHAR(40),
    phone_number    VARCHAR(20),
    currency_id     INTEGER     NOT NULL,
    is_favorite     BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ
);

-- =============================================================================
-- TABLAS DE PRODUCTOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS province (
    id_province    SERIAL PRIMARY KEY,
    cod_province   CHAR(4)      NOT NULL,
    name_province  VARCHAR(100) NOT NULL,
    id_departament INTEGER      NOT NULL REFERENCES departament(id_departament) ON UPDATE CASCADE ON DELETE RESTRICT,
    status         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    CONSTRAINT province_code_uk UNIQUE (cod_province),
    CONSTRAINT province_name_uk UNIQUE (name_province)
);
CREATE INDEX IF NOT EXISTS idx_provincia_name_lower ON province (LOWER(name_province));

CREATE TABLE IF NOT EXISTS district (
    id_district   SERIAL PRIMARY KEY,
    cod_district  CHAR(6)      NOT NULL,
    name_district VARCHAR(100) NOT NULL,
    id_province   INTEGER      NOT NULL REFERENCES province(id_province) ON UPDATE CASCADE ON DELETE RESTRICT,
    status        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT district_code_uk UNIQUE (cod_district),
    CONSTRAINT district_name_uk UNIQUE (name_district, id_province)
);
CREATE INDEX IF NOT EXISTS idx_district_name_lower ON district (LOWER(name_district));

CREATE TABLE IF NOT EXISTS product_uom (
    id               SERIAL PRIMARY KEY,
    code_uom         VARCHAR(10)   NOT NULL UNIQUE,
    name_uom         VARCHAR(100)  NOT NULL,
    id_category_uom  INTEGER       NOT NULL REFERENCES product_category_uom(id),
    factor           DECIMAL(12,6) DEFAULT 1.0,
    rounding_precision DECIMAL(12,6) DEFAULT 0.01,
    is_active        BOOLEAN DEFAULT TRUE,
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT uk_product_uom_code UNIQUE (code_uom)
);

CREATE TABLE IF NOT EXISTS product_templates (
    id                 SERIAL PRIMARY KEY,
    name               VARCHAR(255)  NOT NULL,
    internal_reference VARCHAR(100)  UNIQUE,
    type_product       product_type  NOT NULL,
    id_category        INTEGER       REFERENCES product_category(id),
    id_brand           INTEGER       REFERENCES product_brand(id),
    id_uom             INTEGER       NOT NULL REFERENCES product_uom(id),
    id_currency        INTEGER       NOT NULL REFERENCES currencies(id),
    sale_price         DECIMAL(12,4) DEFAULT 0.0,
    cost               DECIMAL(12,4) DEFAULT 0.0,
    is_igv_exempt      BOOLEAN       DEFAULT FALSE,
    tax_rate           DECIMAL(5,4)  DEFAULT 0.18,
    weight             DECIMAL(10,3),
    volume             DECIMAL(10,3),
    track_inventory    BOOLEAN DEFAULT TRUE,
    use_serial_numbers BOOLEAN DEFAULT FALSE,
    minimum_stock      DECIMAL(12,2),
    maximum_stock      DECIMAL(12,2),
    reorder_point      DECIMAL(12,2),
    lead_time          INTEGER,
    image              TEXT,
    description        TEXT,
    description_sale   TEXT,
    barcode            VARCHAR(100),
    notes              TEXT,
    can_be_sold        BOOLEAN DEFAULT TRUE,
    can_be_purchased   BOOLEAN DEFAULT TRUE,
    allows_price_edit  BOOLEAN DEFAULT FALSE,
    has_variants       BOOLEAN DEFAULT FALSE,
    status             VARCHAR(20) DEFAULT 'active',
    created_at         TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ,
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT uk_product_template_internal_ref UNIQUE (internal_reference)
);

CREATE TABLE IF NOT EXISTS product_variants (
    id                  SERIAL PRIMARY KEY,
    product_template_id INTEGER       NOT NULL REFERENCES product_templates(id),
    sku                 VARCHAR(25)   UNIQUE,
    barcode             VARCHAR(100),
    price_extra         DECIMAL(12,4) DEFAULT 0.0,
    cost_extra          DECIMAL(12,4) DEFAULT 0.0,
    stock               DECIMAL(12,2) DEFAULT 0.0,
    status              VARCHAR(20)   DEFAULT 'active',
    is_default_variant  BOOLEAN       DEFAULT FALSE,
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT uk_product_variant_sku UNIQUE (sku)
);

CREATE TABLE IF NOT EXISTS product_attribute_values (
    id          SERIAL PRIMARY KEY,
    attribute_id INTEGER     NOT NULL REFERENCES product_attributes(id),
    name        VARCHAR(100) NOT NULL,
    html_color  VARCHAR(7),
    sequence    INTEGER      DEFAULT 0,
    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uk_attribute_value UNIQUE (attribute_id, name)
);

CREATE TABLE IF NOT EXISTS product_template_attribute_line (
    id                  SERIAL PRIMARY KEY,
    product_template_id INTEGER NOT NULL REFERENCES product_templates(id),
    attribute_id        INTEGER NOT NULL REFERENCES product_attributes(id),
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_template_attribute UNIQUE (product_template_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS product_variant_value_rel (
    variant_id INTEGER NOT NULL REFERENCES product_variants(id),
    value_id   INTEGER NOT NULL REFERENCES product_attribute_values(id),
    PRIMARY KEY (variant_id, value_id)
);

-- =============================================================================
-- TABLAS CONTABLES
-- =============================================================================

CREATE TABLE IF NOT EXISTS journal_entries (
    id               SERIAL PRIMARY KEY,
    entry_date       DATE                 NOT NULL,
    description      TEXT                 NOT NULL,
    operation_number INTEGER,
    book_correlative INTEGER,
    book_type        accounting_book_type NOT NULL,
    status           entry_status         NOT NULL,
    created_at       TIMESTAMPTZ          NOT NULL,
    updated_at       TIMESTAMPTZ          NOT NULL,
    deleted_at       TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS journal_entry_lines (
    id               SERIAL PRIMARY KEY,
    journal_entry_id INTEGER      NOT NULL REFERENCES journal_entries(id),
    account_id       INTEGER      NOT NULL,
    debit            DECIMAL(12,2) NOT NULL,
    credit           DECIMAL(12,2) NOT NULL,
    description      TEXT
);

CREATE TABLE IF NOT EXISTS journal_entry_documents (
    id                   SERIAL PRIMARY KEY,
    journal_entry_line_id INTEGER      NOT NULL REFERENCES journal_entry_lines(id),
    original_filename    VARCHAR(255)  NOT NULL,
    stored_filename      VARCHAR(255)  NOT NULL,
    file_path            VARCHAR(500)  NOT NULL,
    mime_type            VARCHAR(100)  NOT NULL,
    file_size            BIGINT        NOT NULL,
    uploaded_at          TIMESTAMPTZ   NOT NULL
);
