-- Script de seed para intent_embeddings
-- IMPORTANTE: Los embeddings deben ser generados con el script Python generate_embeddings.py
-- Este script asume que ya tienes los embeddings generados

-- Intent: Consulta de países disponibles para remesas
INSERT INTO intent_embeddings (
    intent_name,
    description,
    example_phrases,
    tool_name,
    prompt_template,
    enabled
) VALUES (
    'REMESA_CONSULTA_PAISES',
    'El cliente quiere saber a qué países puede enviar remesas',
    ARRAY[
        'Buen día estoy interesado en realizar una remesa',
        '¿A qué países puedo enviar dinero?',
        'Quiero hacer una transferencia internacional',
        '¿Cuáles son los destinos disponibles para remesas?',
        'Necesito enviar plata al exterior',
        'Países donde puedo transferir',
        '¿A dónde puedo mandar remesas?'
    ],
    'consultarPaisesDisponibles',
    'Eres un asistente virtual amigable de una empresa de remesas.

El cliente preguntó: {question}

Países disponibles: {data}

Responde de manera clara y amigable, mencionando los países disponibles para enviar remesas.
Sé conciso pero profesional. No uses listas con bullets, menciónalos en un párrafo natural.',
    true
);

-- Intent: Consulta de tasas de cambio (ejemplo futuro)
INSERT INTO intent_embeddings (
    intent_name,
    description,
    example_phrases,
    tool_name,
    prompt_template,
    enabled
) VALUES (
    'CONSULTA_TASAS',
    'El cliente quiere conocer las tasas de cambio actuales',
    ARRAY[
        '¿Cuál es la tasa de cambio actual?',
        '¿A cuánto está el dólar?',
        'Tasas de cambio hoy',
        '¿Cuánto me dan por dólar?'
    ],
    null, -- Tool no implementado aún
    'Eres un asistente virtual de una empresa de remesas.

El cliente preguntó sobre tasas de cambio: {question}

Por el momento, informa al cliente que debe comunicarse con un asesor para conocer las tasas actualizadas.',
    false -- Deshabilitado hasta implementar el tool
);

-- Intent: Saludo general
INSERT INTO intent_embeddings (
    intent_name,
    description,
    example_phrases,
    tool_name,
    prompt_template,
    enabled
) VALUES (
    'SALUDO',
    'El cliente está saludando o iniciando conversación',
    ARRAY[
        'Hola',
        'Buenos días',
        'Buenas tardes',
        'Hola, cómo estás',
        'Hey',
        'Qué tal'
    ],
    null,
    'Eres un asistente virtual amigable de una empresa de remesas.

El cliente te saludó: {question}

Responde de manera cálida y profesional. Preséntate brevemente y pregunta en qué puedes ayudarle.',
    true
);

-- NOTA: Después de ejecutar este script, debes ejecutar el script Python
-- para generar y actualizar los embeddings:
-- python src/main/resources/sql/generate_embeddings.py
