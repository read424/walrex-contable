package org.walrex.infrastructure.adapter.outbound.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.quarkiverse.langchain4j.ModelName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.walrex.application.port.output.ChatOutputPort;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Adaptador para chat usando Ollama con Phi3
 * Incluye circuit breaker y timeout para robustez
 * Marcado como @Alternative para que Groq sea el default
 */
@Slf4j
@ApplicationScoped
@Alternative
@RegisterForReflection
public class OllamaChatAdapter implements ChatOutputPort {

    @Inject
    @ModelName("ollama-chat")
    ChatModel chatModel;

    @Override
    @Timeout(value = 60, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 2
    )
    @CircuitBreakerName("ollama-chat")
    public Uni<String> generateResponse(String prompt) {
        log.debug("Generating response for prompt: {}", prompt.substring(0, Math.min(100, prompt.length())));

        return Uni.createFrom().item(() -> {
            try {
                ChatResponse response = chatModel.chat(UserMessage.from(prompt));
                String responseText = response.aiMessage().text();
                log.debug("Generated response: {}", responseText.substring(0, Math.min(100, responseText.length())));
                return responseText;
            } catch (Exception e) {
                log.error("Error generating chat response", e);
                throw new RuntimeException("Failed to generate chat response", e);
            }
        })
        // Run blocking Ollama call on worker pool, Mutiny preserves context automatically
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @Override
    @Timeout(value = 60, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 2
    )
    @CircuitBreakerName("ollama-chat-system")
    public Uni<String> generateResponse(String systemPrompt, String userMessage) {
        log.debug("Generating response with system prompt and user message");

        return Uni.createFrom().item(() -> {
            try {
                List<ChatMessage> messages = List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userMessage)
                );

                ChatResponse response = chatModel.chat(messages);
                String responseText = response.aiMessage().text();

                log.debug("Generated response: {}", responseText.substring(0, Math.min(100, responseText.length())));
                return responseText;
            } catch (Exception e) {
                log.error("Error generating chat response with system prompt", e);
                throw new RuntimeException("Failed to generate chat response", e);
            }
        })
        // Run blocking Ollama call on worker pool, Mutiny preserves context automatically
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
