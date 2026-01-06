package org.walrex.domain.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.walrex.application.port.output.ChatOutputPort;
import org.walrex.domain.exception.LLMNotFoundException;

/**
 * Factory para seleccionar dinámicamente el LLM a utilizar.
 * Implementa Strategy Pattern para alternar entre Groq y Ollama.
 */
@Slf4j
@ApplicationScoped
public class LLMStrategyFactory {

    @ConfigProperty(name = "rag.llm.default-provider", defaultValue = "groq")
    String defaultProvider;

    @Inject
    @Any
    Instance<ChatOutputPort> chatAdapters;

    /**
     * Obtiene el adapter LLM configurado por defecto.
     */
    public ChatOutputPort getDefaultLLM() {
        return getLLM(defaultProvider);
    }

    /**
     * Obtiene un adapter LLM específico por nombre.
     * @param providerName "groq" o "ollama"
     */
    public ChatOutputPort getLLM(String providerName) {
        log.debug("Selecting LLM provider: {}", providerName);

        if (providerName == null || providerName.isBlank()) {
            providerName = defaultProvider;
        }

        // Groq es el default (no tiene @Alternative)
        if ("groq".equalsIgnoreCase(providerName)) {
            return chatAdapters.stream()
                .filter(adapter -> adapter.getClass().getSimpleName().contains("Groq"))
                .findFirst()
                .orElseThrow(() -> new LLMNotFoundException("groq"));
        }

        // Ollama tiene @Alternative, necesita select manual
        if ("ollama".equalsIgnoreCase(providerName)) {
            return chatAdapters.select(Alternative.Literal.INSTANCE).stream()
                .filter(adapter -> adapter.getClass().getSimpleName().contains("Ollama"))
                .findFirst()
                .orElseThrow(() -> new LLMNotFoundException("ollama"));
        }

        throw new LLMNotFoundException(providerName);
    }
}
