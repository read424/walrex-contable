-- Tabla para almacenar intents con sus embeddings para búsqueda semántica
CREATE TABLE IF NOT EXISTS intent_embeddings (
    id BIGSERIAL PRIMARY KEY,
    intent_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    example_phrases TEXT[], -- Frases de ejemplo para este intent
    embedding vector(1024), -- mxbai-embed-large genera 1024 dimensiones
    tool_name VARCHAR(100), -- Nombre del tool MCP a ejecutar
    prompt_template TEXT, -- Template para RAG
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Índice para búsqueda por similitud de coseno (más eficiente para embeddings normalizados)
CREATE INDEX IF NOT EXISTS idx_intent_embedding_cosine ON intent_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Índice adicional para consultas por nombre
CREATE INDEX IF NOT EXISTS idx_intent_name ON intent_embeddings(intent_name);

-- Índice para filtrar por enabled
CREATE INDEX IF NOT EXISTS idx_intent_enabled ON intent_embeddings(enabled) WHERE enabled = TRUE;

-- Comentarios para documentación
COMMENT ON TABLE intent_embeddings IS 'Almacena intenciones de usuario con embeddings para matching semántico';
COMMENT ON COLUMN intent_embeddings.embedding IS 'Vector de 1024 dimensiones generado por mxbai-embed-large';
COMMENT ON COLUMN intent_embeddings.tool_name IS 'Nombre del tool MCP a ejecutar cuando se detecta este intent';
COMMENT ON COLUMN intent_embeddings.prompt_template IS 'Template con placeholders para construir prompt RAG: {data}, {question}';
