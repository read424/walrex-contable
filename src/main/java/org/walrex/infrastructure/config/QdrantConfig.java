package org.walrex.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Configuración de beans de infraestructura para Qdrant.
 * Produce el cliente de Qdrant como bean CDI.
 */
@Slf4j
@ApplicationScoped
public class QdrantConfig {

    @ConfigProperty(name = "qdrant.host", defaultValue = "localhost")
    String qdrantHost;

    @ConfigProperty(name = "qdrant.port", defaultValue = "6334")
    Integer qdrantPort;

    @ConfigProperty(name = "qdrant.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "qdrant.use-tls", defaultValue = "false")
    Boolean useTls;

    /**
     * Produce el cliente nativo de Qdrant para operaciones de bajo nivel.
     * El EmbeddingStore de LangChain4j usa su propia configuración interna.
     */
    @Produces
    @ApplicationScoped
    public QdrantClient qdrantClient() {
        log.info("Creating QdrantClient for {}:{}", qdrantHost, qdrantPort);

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls);

        apiKey.ifPresent(builder::withApiKey);

        return new QdrantClient(builder.build());
    }
}
