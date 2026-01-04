-- Agregar campo embeddings_synced a la tabla accounts
-- Este campo indica si la cuenta ya fue sincronizada con Qdrant (base de datos vectorial)

ALTER TABLE accounts
ADD COLUMN embeddings_synced BOOLEAN NOT NULL DEFAULT FALSE;

-- Crear índice para optimizar consultas de cuentas no sincronizadas
CREATE INDEX idx_accounts_embeddings_synced
ON accounts(embeddings_synced)
WHERE embeddings_synced = FALSE AND is_active = TRUE;

-- Comentarios para documentación
COMMENT ON COLUMN accounts.embeddings_synced IS 'Indica si la cuenta ya fue procesada y sincronizada con la base de datos vectorial Qdrant';
COMMENT ON INDEX idx_accounts_embeddings_synced IS 'Índice parcial para optimizar consultas de cuentas pendientes de sincronización';
