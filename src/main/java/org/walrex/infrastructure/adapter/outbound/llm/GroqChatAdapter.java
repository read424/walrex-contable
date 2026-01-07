package org.walrex.infrastructure.adapter.outbound.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.walrex.application.port.output.ChatOutputPort;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Adaptador para chat usando Groq (via OpenAI API)
 * Incluye circuit breaker y timeout para robustez
 * Este es el adaptador predeterminado
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class GroqChatAdapter implements ChatOutputPort {

    @Inject
    @ModelName("groq-chat")
    ChatModel chatModel;

    @Override
    @Timeout(value = 60, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 2
    )
    @CircuitBreakerName("groq-chat")
    public Uni<String> generateResponse(String prompt) {
        log.debug("Generating response with Groq for prompt: {}", prompt.substring(0, Math.min(100, prompt.length())));

        return Uni.createFrom().item(() -> {
            try {
                ChatResponse response = chatModel.chat(UserMessage.from(prompt));
                String responseText = response.aiMessage().text();
                log.debug("Generated response from Groq: {}", responseText.substring(0, Math.min(100, responseText.length())));
                return responseText;
            } catch (Exception e) {
                log.error("Error generating chat response with Groq", e);
                throw new RuntimeException("Failed to generate chat response with Groq", e);
            }
        })
        // Run blocking Groq call on worker pool, Mutiny preserves context automatically
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
    @CircuitBreakerName("groq-chat-system")
    public Uni<String> generateResponse(String systemPrompt, String userMessage) {
        log.debug("Generating response with Groq using system prompt and user message");

        return Uni.createFrom().item(() -> {
            try {
                List<ChatMessage> messages = List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userMessage)
                );

                ChatResponse response = chatModel.chat(messages);
                String responseText = response.aiMessage().text();

                log.debug("Generated response from Groq: {}", responseText.substring(0, Math.min(100, responseText.length())));
                return responseText;
            } catch (Exception e) {
                log.error("Error generating chat response with Groq and system prompt", e);
                throw new RuntimeException("Failed to generate chat response with Groq", e);
            }
        })
        // Run blocking Groq call on worker pool, Mutiny preserves context automatically
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
