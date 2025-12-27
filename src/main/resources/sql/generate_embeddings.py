#!/usr/bin/env python3
"""
Script para generar embeddings de los intents usando Ollama
y actualizarlos en la base de datos PostgreSQL

Requisitos:
- pip install psycopg2-binary requests
- Ollama ejecutándose en localhost:11434
- Modelo mxbai-embed-large instalado en Ollama
"""

import json
import requests
import psycopg2
from typing import List

# Configuración
OLLAMA_URL = "http://localhost:11434/api/embeddings"
EMBEDDING_MODEL = "mxbai-embed-large"

# Configuración de base de datos
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 5432,
    "database": "walrex_db",
    "user": "postgres",
    "password": "12345"
}


def generate_embedding(text: str) -> List[float]:
    """Genera embedding usando Ollama"""
    print(f"Generating embedding for: {text[:50]}...")

    payload = {
        "model": EMBEDDING_MODEL,
        "prompt": text
    }

    response = requests.post(OLLAMA_URL, json=payload)
    response.raise_for_status()

    embedding = response.json()["embedding"]
    print(f"  ✓ Generated {len(embedding)} dimensions")
    return embedding


def generate_intent_embedding(example_phrases: List[str]) -> List[float]:
    """
    Genera un embedding representativo para un intent
    Promedia los embeddings de todas las frases de ejemplo
    """
    print(f"Processing {len(example_phrases)} example phrases...")

    embeddings = []
    for phrase in example_phrases:
        emb = generate_embedding(phrase)
        embeddings.append(emb)

    # Promediar todos los embeddings
    avg_embedding = []
    for i in range(len(embeddings[0])):
        avg = sum(emb[i] for emb in embeddings) / len(embeddings)
        avg_embedding.append(avg)

    return avg_embedding


def update_intent_embeddings():
    """Actualiza los embeddings de todos los intents en la BD"""
    print("Connecting to database...")
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()

    try:
        # Obtener todos los intents sin embedding
        cursor.execute("""
            SELECT id, intent_name, example_phrases
            FROM intent_embeddings
            WHERE embedding IS NULL OR enabled = true
        """)

        intents = cursor.fetchall()
        print(f"\nFound {len(intents)} intents to process\n")

        for intent_id, intent_name, example_phrases in intents:
            print(f"Processing intent: {intent_name}")
            print(f"  ID: {intent_id}")

            # Generar embedding
            embedding = generate_intent_embedding(example_phrases)

            # Formatear para PostgreSQL vector
            vector_str = "[" + ",".join(str(x) for x in embedding) + "]"

            # Actualizar en BD
            cursor.execute("""
                UPDATE intent_embeddings
                SET embedding = %s::vector
                WHERE id = %s
            """, (vector_str, intent_id))

            print(f"  ✓ Updated embedding in database\n")

        conn.commit()
        print("✅ All embeddings generated and updated successfully!")

    except Exception as e:
        conn.rollback()
        print(f"❌ Error: {e}")
        raise
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    print("=" * 60)
    print("Intent Embeddings Generator")
    print("=" * 60)
    print()

    try:
        # Verificar que Ollama está disponible
        response = requests.get("http://localhost:11434/api/tags")
        response.raise_for_status()
        print("✓ Ollama is running")
        print()

        # Generar y actualizar embeddings
        update_intent_embeddings()

    except requests.exceptions.ConnectionError:
        print("❌ Error: Cannot connect to Ollama")
        print("   Make sure Ollama is running: http://localhost:11434")
    except psycopg2.Error as e:
        print(f"❌ Database error: {e}")
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        raise
