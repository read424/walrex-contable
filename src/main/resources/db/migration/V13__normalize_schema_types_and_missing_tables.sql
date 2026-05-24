-- V13: Normalize column types to match Hibernate entity definitions
--      and create tables missing from the managed schema.
--
-- All ALTER COLUMN TYPE operations use USING casts (widening or compatible).
-- CREATE TABLE statements use IF NOT EXISTS for idempotency.
-- Unique constraints are added inside a DO block to skip existing ones.

-- ─────────────────────────────────────────────────────────────────────────────
-- intent_embeddings: example_phrases text → TEXT[]
-- Existing text data cannot be safely cast to TEXT[]; reset to NULL.
-- Embeddings sync re-populates this column on startup.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'intent_embeddings'
          AND column_name  = 'example_phrases'
          AND data_type   <> 'ARRAY'
    ) THEN
        UPDATE public.intent_embeddings SET example_phrases = NULL;
        ALTER TABLE public.intent_embeddings
            ALTER COLUMN example_phrases TYPE TEXT[] USING NULL::TEXT[];
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- merchant_qr: create table (entity MerchantQrEntity)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.merchant_qr (
    id                         bigserial    NOT NULL,
    name                       varchar(100) NOT NULL,
    merchant_name              varchar(100),
    merchant_city              varchar(100),
    mcc                        varchar(4),
    currency                   varchar(3),
    country_code               varchar(2),
    payload_format_indicator   varchar(2),
    point_of_initiation_method varchar(2),
    account_info               text,
    created_at                 timestamptz  NOT NULL,
    CONSTRAINT merchant_qr_pk PRIMARY KEY (id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- refresh_tokens: create table (entity RefreshTokenEntity)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.refresh_tokens (
    id                 serial4     NOT NULL,
    user_id            integer     NOT NULL,
    device_id          integer,
    refresh_token_hash varchar     NOT NULL,
    expires_at         timestamptz NOT NULL,
    revoked_at         timestamptz,
    created_at         timestamptz,
    CONSTRAINT refresh_tokens_pk PRIMARY KEY (id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- user_device_token: create table (entity DeviceTokenEntity)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.user_device_token (
    id         serial4      NOT NULL,
    user_id    integer      NOT NULL,
    token      varchar(512) NOT NULL,
    platform   varchar(20)  NOT NULL,
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamptz,
    updated_at timestamptz,
    CONSTRAINT user_device_token_pk PRIMARY KEY (id),
    CONSTRAINT uq_user_device_token_token UNIQUE (token)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ocupaciones: id serial4/int4 → bigint, status → integer
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.ocupaciones
    ALTER COLUMN id     TYPE bigint  USING id::bigint,
    ALTER COLUMN status TYPE integer USING status::integer;

-- ─────────────────────────────────────────────────────────────────────────────
-- otp: id serial4/int4 → bigint
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.otp
    ALTER COLUMN id TYPE bigint USING id::bigint;

-- ─────────────────────────────────────────────────────────────────────────────
-- price_exchange: updated_at → timetz (OffsetTime in Java)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.price_exchange
    ALTER COLUMN updated_at TYPE timetz USING updated_at::timetz;

-- ─────────────────────────────────────────────────────────────────────────────
-- product_templates: cost/sale_price → DECIMAL(12,4), tax_rate → DECIMAL(5,4)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.product_templates
    ALTER COLUMN cost       TYPE decimal(12,4) USING cost::decimal(12,4),
    ALTER COLUMN sale_price TYPE decimal(12,4) USING sale_price::decimal(12,4),
    ALTER COLUMN tax_rate   TYPE decimal(5,4)  USING tax_rate::decimal(5,4);

-- ─────────────────────────────────────────────────────────────────────────────
-- product_uom: factor/rounding_precision → DECIMAL(12,6)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.product_uom
    ALTER COLUMN factor             TYPE decimal(12,6) USING factor::decimal(12,6),
    ALTER COLUMN rounding_precision TYPE decimal(12,6) USING rounding_precision::decimal(12,6);

-- ─────────────────────────────────────────────────────────────────────────────
-- product_variants: cost_extra/price_extra → DECIMAL(12,4), stock → DECIMAL(12,2)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.product_variants
    ALTER COLUMN cost_extra  TYPE decimal(12,4) USING cost_extra::decimal(12,4),
    ALTER COLUMN price_extra TYPE decimal(12,4) USING price_extra::decimal(12,4),
    ALTER COLUMN stock       TYPE decimal(12,2) USING stock::decimal(12,2);

-- ─────────────────────────────────────────────────────────────────────────────
-- users: status/pin_attempts → integer
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.users
    ALTER COLUMN status       TYPE integer USING status::integer,
    ALTER COLUMN pin_attempts TYPE integer USING pin_attempts::integer;

-- ─────────────────────────────────────────────────────────────────────────────
-- Unique constraints — skip if already present
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_product_template_internal_ref' AND contype = 'u'
    ) THEN
        ALTER TABLE public.product_templates
            ADD CONSTRAINT uk_product_template_internal_ref UNIQUE (internal_reference);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_product_uom_code' AND contype = 'u'
    ) THEN
        ALTER TABLE public.product_uom
            ADD CONSTRAINT uk_product_uom_code UNIQUE (code_uom);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_product_variant_sku' AND contype = 'u'
    ) THEN
        ALTER TABLE public.product_variants
            ADD CONSTRAINT uk_product_variant_sku UNIQUE (sku);
    END IF;
END $$;
