-- =============================================================================
-- V3__seed_monedas.sql
-- Datos iniciales: tabla currencies
-- =============================================================================

-- ── currencies ────────────────────────────────────────────────────────────────
INSERT INTO public.currencies (id, code_iso3, name, status, created_at, updated_at, numericcode, deleted_at, symbol) VALUES
    (3,  'VES', 'Venezuela Bolivar',      '1', '2024-09-08 20:37:00.36829-05',  '2024-09-08 20:37:00.372726-05', 937, NULL, 'Bs'),
    (4,  'PEN', 'Peru Nuevo Sol',         '1', '2024-09-08 20:37:00.36829-05',  '2024-09-08 20:37:00.372726-05', 604, NULL, 'S/'),
    (5,  'USD', 'United States Dollar',   '1', '2024-09-08 21:01:51.792661-05', '2024-09-08 21:01:51.792661-05', 840, NULL, '$'),
    (6,  'COP', 'Pesos Colombianos',      '1', '2024-11-23 18:26:27.055537-05', '2024-11-23 18:26:27.055537-05', 170, NULL, '$'),
    (7,  'CLP', 'Peso Chileno',           '1', '2025-08-21 07:07:46.706849-05', '2025-08-21 07:07:46.706849-05', 152, NULL, '$'),
    (8,  'EUR', 'Euro',                   '1', NULL,                            NULL,                            978, NULL, NULL),
    (9,  'BRL', 'REAL BRASILEÑO',         '1', NULL,                            NULL,                            986, NULL, '$'),
    (10, 'MXN', 'Peso Mexicano',          '1', '2026-05-10 13:27:37.986687-05', '2026-05-10 13:27:37.986687-05', 484, NULL, 'MX$')
ON CONFLICT (id) DO NOTHING;

-- Ajustar la secuencia al máximo ID insertado
SELECT setval('public.currencies_id_seq', (SELECT MAX(id) FROM public.currencies));
