-- =======================================================================
-- Migration: Create junction table for country_currencies <-> banks
-- Purpose: Link payment methods (banks) to country_currencies for
--          Binance P2P payment method filtering
-- Version: V7
-- Created: 2025-12-28
-- =======================================================================

-- Create junction table
CREATE TABLE IF NOT EXISTS public.country_currency_payment_methods (
    id BIGSERIAL PRIMARY KEY,
    id_country_currency BIGINT NOT NULL,
    id_bank BIGINT NOT NULL,
    is_active CHAR(1) NOT NULL DEFAULT '1',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ccpm_country_currency FOREIGN KEY (id_country_currency)
        REFERENCES public.country_currencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_ccpm_bank FOREIGN KEY (id_bank)
        REFERENCES public.banks(id) ON DELETE CASCADE,
    CONSTRAINT uk_ccpm_country_currency_bank UNIQUE (id_country_currency, id_bank),
    CONSTRAINT ck_ccpm_active CHECK (is_active IN ('0', '1'))
);

-- Table and column comments
COMMENT ON TABLE public.country_currency_payment_methods IS
    'Junction table linking country_currencies to banks (payment methods) for Binance P2P queries. ' ||
    'Each row represents an active payment method available for a specific country-currency combination.';

COMMENT ON COLUMN public.country_currency_payment_methods.id_country_currency IS
    'Foreign key to country_currencies table (e.g., Peru-PEN, Venezuela-VES)';

COMMENT ON COLUMN public.country_currency_payment_methods.id_bank IS
    'Foreign key to banks table. Uses bank.name_pay_binance as the Binance payment method code.';

COMMENT ON COLUMN public.country_currency_payment_methods.is_active IS
    'Active flag: 1=payment method is active and will be used in Binance queries, 0=disabled';

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_ccpm_country_currency
    ON public.country_currency_payment_methods(id_country_currency)
    WHERE is_active = '1';

CREATE INDEX IF NOT EXISTS idx_ccpm_bank
    ON public.country_currency_payment_methods(id_bank);

CREATE INDEX IF NOT EXISTS idx_ccpm_active
    ON public.country_currency_payment_methods(is_active);

CREATE INDEX IF NOT EXISTS idx_ccpm_lookup
    ON public.country_currency_payment_methods(id_country_currency, is_active)
    INCLUDE (id_bank);

-- =======================================================================
-- SAMPLE DATA: Payment methods for Peru and Venezuela
-- =======================================================================
-- NOTE: Adjust based on your actual database values for country_currency IDs
-- This script attempts to auto-populate based on existing data

-- Payment methods for Peru (PEN and USD)
-- Associates all active Peruvian banks with Binance payment codes to Peru currencies
INSERT INTO public.country_currency_payment_methods
    (id_country_currency, id_bank, is_active)
SELECT
    cc.id as id_country_currency,
    b.id as id_bank,
    '1' as is_active
FROM public.country_currencies cc
JOIN public.country c ON cc.country_id = c.id
JOIN public.banks b ON b.id_country = c.id
WHERE c.alphabetic_code_2 = 'PE'       -- Peru
  AND b.status = '1'                    -- Active bank
  AND b.name_pay_binance IS NOT NULL    -- Has Binance payment code
  AND cc.is_operational = true          -- Operational currency
ON CONFLICT (id_country_currency, id_bank) DO NOTHING;

-- Payment methods for Venezuela (VES)
-- Associates all active Venezuelan banks with Binance payment codes to Venezuela currencies
INSERT INTO public.country_currency_payment_methods
    (id_country_currency, id_bank, is_active)
SELECT
    cc.id as id_country_currency,
    b.id as id_bank,
    '1' as is_active
FROM public.country_currencies cc
JOIN public.country c ON cc.country_id = c.id
JOIN public.banks b ON b.id_country = c.id
WHERE c.alphabetic_code_2 = 'VE'       -- Venezuela
  AND b.status = '1'                    -- Active bank
  AND b.name_pay_binance IS NOT NULL    -- Has Binance payment code
  AND cc.is_operational = true          -- Operational currency
ON CONFLICT (id_country_currency, id_bank) DO NOTHING;

-- =======================================================================
-- VERIFICATION QUERIES (commented out, uncomment to run manually)
-- =======================================================================

-- Check payment methods configuration per country-currency
/*
SELECT
    cc.id as country_currency_id,
    c.name as country,
    cur.alphabetic_code as currency,
    b.det_name as bank_name,
    b.name_pay_binance as binance_code,
    ccpm.is_active
FROM country_currency_payment_methods ccpm
JOIN country_currencies cc ON ccpm.id_country_currency = cc.id
JOIN country c ON cc.country_id = c.id
JOIN currencies cur ON cc.currency_id = cur.id
JOIN banks b ON ccpm.id_bank = b.id
WHERE ccpm.is_active = '1'
ORDER BY country, currency, bank_name;
*/

-- Count payment methods per country_currency
/*
SELECT
    cc.id,
    c.name as country,
    cur.alphabetic_code as currency,
    COUNT(ccpm.id) as payment_method_count
FROM country_currencies cc
JOIN country c ON cc.country_id = c.id
JOIN currencies cur ON cc.currency_id = cur.id
LEFT JOIN country_currency_payment_methods ccpm
    ON cc.id = ccpm.id_country_currency AND ccpm.is_active = '1'
WHERE cc.is_operational = true
GROUP BY cc.id, c.name, cur.alphabetic_code
ORDER BY c.name, cur.alphabetic_code;
*/
