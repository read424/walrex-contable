-- =============================================================================
-- V5__seed_instituciones_financieras.sql
-- Datos iniciales: tabla financial_institution
-- =============================================================================

-- ── financial_institution ─────────────────────────────────────────────────────
INSERT INTO public.financial_institution (id, sigla, det_name, id_country, status, created_at, updated_at, codigo, name_pay_binance) VALUES
    (1,  'BCP',     'Banco Nacional de Credito del Peru', 1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'CreditBankofPeru'),
    (2,  'SCT',     'Banco Scotiabank',                   1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Scotiabank Peru'),
    (3,  'ITBK',    'Banco Interbank',                    1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Interbank'),
    (4,  'BBVA',    'Banco BBVA',                         1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'BBVA Peú'),
    (5,  'BDV',     'Banco de Venezuela',                 2, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Bank Transfer'),
    (7,  'BCARIBE', 'BANCARIBE',                          2, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, NULL),
    (8,  'BBVAVE',  'Banco Provincial BBVA',              2, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, NULL),
    (9,  'BANESC',  'Banco Banesco',                      2, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Banesco'),
    (10, 'MERCAN',  'Banco Mercantil',                    2, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Mercantil'),
    (11, 'YAPE',    'Yape',                               1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Yape'),
    (12, 'PLIN',    'PLIN',                               1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'Plin'),
    (15, 'BCP Peru','Banco Nacional de Credito',          1, '1', '2026-01-13 06:10:42.723678-05', '2026-01-13 06:10:42.723678-05', NULL, 'BancoDeCredito'),
    (16, 'FLBCLP',  'Falabella Chile',                    5, '1', '2026-01-30 11:13:19.511661-05', '2026-01-30 11:13:19.511661-05', NULL, 'BancoFalabella'),
    (17, 'BPCHECU', 'Banco Pichincha',                    7, '1', '2026-02-11 15:36:04.640042-05', '2026-02-11 15:36:04.640042-05', NULL, 'BancoPichincha'),
    (19, 'BINAN',   'Binance',                            2, '1', '2026-02-14 05:29:35.24298-05',  '2026-02-14 05:29:35.24298-05',  NULL, NULL),
    (20, 'Nequi',   'Nequi',                              4, '1', '2026-02-15 07:14:12.366934-05', '2026-02-15 07:14:12.366934-05', NULL, 'Nequi')
ON CONFLICT (id) DO NOTHING;

-- Ajustar secuencia al máximo ID insertado
SELECT setval('public.financial_institution_id_seq', (SELECT MAX(id) FROM public.financial_institution));
