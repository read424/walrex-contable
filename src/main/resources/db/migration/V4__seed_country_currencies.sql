-- =============================================================================
-- V4__seed_country_currencies.sql
-- Datos iniciales: tabla country_currencies
-- =============================================================================

-- ── country_currencies ────────────────────────────────────────────────────────
INSERT INTO public.country_currencies (id, country_id, currency_id, is_primary, is_operational, effective_date, created_at) VALUES
    (1, 1, 4, false, true, '2025-12-11', '2025-12-12 03:25:59.930085'),
    (2, 1, 5, true,  true, '2025-12-11', '2025-12-12 03:29:51.352448'),
    (3, 2, 3, true,  true, '2025-12-14', '2025-12-15 03:07:24.74313'),
    (4, 4, 6, false, true, NULL,         '2025-12-27 11:04:54.011753'),
    (5, 5, 7, false, true, NULL,         '2026-01-30 11:11:26.581168'),
    (6, 7, 5, true,  true, '2026-02-11', '2026-02-11 15:31:35.972152'),
    (7, 6, 9, true,  true, NULL,         '2026-05-10 13:11:06.116315'),
    (8, 8, 8, true,  true, NULL,         '2026-05-10 13:13:17.788265'),
    (9, 9, 10, true, true, NULL,         '2026-05-10 13:28:04.10113')
ON CONFLICT (id) DO NOTHING;

-- Ajustar la secuencia al máximo ID insertado
SELECT setval('public.country_currencies_id_seq', (SELECT MAX(id) FROM public.country_currencies));
