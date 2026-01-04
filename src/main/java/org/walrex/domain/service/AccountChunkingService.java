package org.walrex.domain.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.domain.model.AccountChunk;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.domain.service.EmbeddingGeneratorService;
import org.walrex.infrastructure.adapter.logging.LogExecutionTime;

/**
 * Servicio de dominio para generar chunks y embeddings de cuentas contables.
 * Implementa la lógica de negocio para convertir cuentas en texto semántico.
 */
@Slf4j
@ApplicationScoped
public class AccountChunkingService {

    @Inject
    EmbeddingGeneratorService embeddingGeneratorService;

    /**
     * Crea un chunk de texto formateado a partir de una cuenta contable.
     * Formato: "Código: [code]. Cuenta: [name]. Tipo: [type]. Naturaleza: [normal_side].
     * Descripción: Esta cuenta se utiliza para registrar [name] de naturaleza [normal_side]."
     *
     * @param account Cuenta contable
     * @return Texto del chunk formateado
     */
    @WithSpan("AccountChunkingService.createChunk")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG)
    public String createChunk(AccountingAccount account) {
        log.debug("Creating chunk for account: {} ({})", account.getCode(), account.getName());

        String type = account.getType() != null ? account.getType().name() : "N/A";
        String normalSide = account.getNormalSide() != null ? account.getNormalSide().name() : "N/A";

        String chunk = String.format(
                "Código: %s. Cuenta: %s. Tipo: %s. Naturaleza: %s. Descripción: Esta cuenta se utiliza para registrar %s de naturaleza %s.",
                account.getCode(),
                account.getName(),
                type,
                normalSide,
                account.getName(),
                normalSide
        );

        log.trace("Generated chunk: {}", chunk);
        return chunk;
    }

    /**
     * Genera un embedding vectorial a partir de un texto.
     *
     * @param text Texto para generar el embedding
     * @return Uni con el array de floats del embedding
     */
    @WithSpan("AccountChunkingService.generateEmbedding")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.DEBUG)
    public Uni<float[]> generateEmbedding(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());

        return embeddingGeneratorService.generate(text)
                .onItem().transform(embedding -> {
                    log.debug("Embedding generated successfully with dimension: {}", embedding.length);
                    return embedding;
                })
                .onFailure().invoke(throwable ->
                        log.error("Failed to generate embedding", throwable)
                );
    }

    /**
     * Crea un AccountChunk completo con texto y embedding.
     *
     * @param account Cuenta contable
     * @return Uni con el AccountChunk generado
     */
    @WithSpan("AccountChunkingService.createAccountChunk")
    @LogExecutionTime(value = LogExecutionTime.LogLevel.INFO, logParameters = true)
    public Uni<AccountChunk> createAccountChunk(AccountingAccount account) {
        log.info("Creating account chunk for account ID: {} ({})", account.getId(), account.getCode());

        String chunkText = createChunk(account);

        return generateEmbedding(chunkText)
                .onItem().transform(embedding -> {
                    AccountChunk chunk = AccountChunk.builder()
                            .accountId(account.getId())
                            .code(account.getCode())
                            .name(account.getName())
                            .type(account.getType())
                            .normalSide(account.getNormalSide())
                            .chunkText(chunkText)
                            .embedding(embedding)
                            .active(account.getActive())
                            .build();

                    log.debug("Account chunk created successfully for account: {}", account.getCode());
                    return chunk;
                });
    }
}
