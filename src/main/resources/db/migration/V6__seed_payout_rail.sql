-- =============================================================================
-- V6__seed_payout_rail.sql
-- Datos iniciales: tabla payout_rail
-- =============================================================================

-- ── payout_rail ───────────────────────────────────────────────────────────────
INSERT INTO public.payout_rail (id, code, description) VALUES
    (2, 'BANK_TRANSFER',  'Transferencias Bancarias'),
    (3, 'MOBILE_PAYMENT', 'Pagos Moviles'),
    (4, 'WALLET',         'Billeteras Electrónicas'),
    (5, 'CASH_PICKUP',    'Retiro en Tiendas')
ON CONFLICT (id) DO NOTHING;

-- Ajustar secuencia al máximo ID insertado
SELECT setval('public.payout_rail_id_seq', (SELECT MAX(id) FROM public.payout_rail));
