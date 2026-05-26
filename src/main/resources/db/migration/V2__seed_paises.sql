-- =============================================================================
-- V2__seed_paises.sql
-- Datos iniciales: tabla country
-- =============================================================================

-- ── country ──────────────────────────────────────────────────────────────────
INSERT INTO public.country (id, code_iso2, code_iso3, name_iso, code_phone_iso, status, created_at, updated_at, unicode_flag, deleted_at, numeric_code) VALUES
    (1, 'PE', 'PER', 'PERU',                        '+51',  '1', '2024-09-08 20:24:15.805005-05', '2024-09-08 20:24:15.805005-05', 'U+1F1F5 U+1F1EA', NULL, NULL),
    (2, 'VE', 'VEN', 'VENEZUELA',                   '+58',  '1', '2024-09-08 20:24:15.81163-05',  '2025-12-14 21:21:03.159436-05', 'U+1F1FB U+1F1EA', NULL, 862),
    (3, 'US', 'USA', 'ESTADOS UNIDOS DE AMERICA',   '+1',   '1', '2024-09-08 21:01:08.654191-05', '2024-09-08 21:01:08.654191-05', 'U+1F1FA U+1F1F8', NULL, NULL),
    (4, 'CO', 'COP', 'COLOMBIA',                    '+53',  '1', '2024-11-23 18:26:06.340531-05', '2024-11-23 18:26:06.340531-05', 'U+1F1E8 U+1F1F4', NULL, NULL),
    (5, 'CL', 'CHL', 'CHILE',                       '+56',  '1', '2025-08-21 07:07:34.516124-05', '2025-08-21 07:07:34.516124-05', 'U+1F1E8 U+1F1F1', NULL, NULL),
    (6, 'BR', 'BRA', 'BRASIL',                      '+55',  '1', '2025-11-13 06:54:47.056679-05', '2025-11-13 06:54:47.056679-05', 'U+1F1E7 U+1F1F7', NULL, NULL),
    (7, 'EC', 'ECU', 'ECUADOR',                     '+593', '1', '2026-02-11 15:15:55.725827-05', '2026-02-11 15:15:55.725827-05', 'U+1F1E8 U+1F1E8', NULL, NULL),
    (8, 'ES', 'ESP', 'ESPAÑA',                      '+34',  '1', '2026-05-10 13:12:32.074113-05', '2026-05-10 13:12:32.074113-05', NULL,              NULL, NULL),
    (9, 'MX', 'MEX', 'MÉXICO',                      '+52',  '1', '2026-05-10 13:24:09.614065-05', '2026-05-10 13:24:09.614065-05', NULL,              NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- Ajustar la secuencia al máximo ID insertado para evitar conflictos futuros
SELECT setval('public.country_id_seq', (SELECT MAX(id) FROM public.country));
