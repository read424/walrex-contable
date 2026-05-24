CREATE TABLE IF NOT EXISTS market_price_tick (
    id          BIGSERIAL    PRIMARY KEY,
    provider    VARCHAR(20)  NOT NULL,
    symbol      VARCHAR(50)  NOT NULL,
    currency_base  VARCHAR(10) NOT NULL,
    currency_quote VARCHAR(10) NOT NULL,
    price       DECIMAL(20,8) NOT NULL,
    event_type  VARCHAR(20)  NOT NULL DEFAULT 'TICK',
    change_pct  DECIMAL(10,6),
    recorded_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mpt_symbol_time   ON market_price_tick(symbol, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_mpt_provider_time ON market_price_tick(provider, recorded_at DESC);
