package org.walrex.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ExecutionException;

/**
 * Inicializador de infraestructura para Qdrant.
 * Crea la colección de embeddings al arrancar la aplicación si no existe.
 *
 * Responsabilidad única: Inicialización de recursos de base de datos vectorial.
 */
@Slf4j
@Startup
@ApplicationScoped
public class QdrantCollectionInitializer {

    @Inject
    QdrantClient qdrantClient;

    @ConfigProperty(name = "qdrant.collection.name", defaultValue = "accounting_data")
    String collectionName;

    @ConfigProperty(name = "qdrant.dimension", defaultValue = "1024")
    Integer dimension;

    @ConfigProperty(name = "qdrant.distance", defaultValue = "Cosine")
    String distance;

    void onStart(@Observes StartupEvent event) {
        try {

            log.info("Checking if Qdrant collection '{}' exists...", collectionName);

            // Verificar si la colección existe
            boolean exists = collectionExists();

            if (!exists) {
                log.info("Collection '{}' does not exist. Creating...", collectionName);
                createCollection();
                log.info("✅ Collection '{}' created successfully with dimension {} and distance {}",
                        collectionName, dimension, distance);
            } else {
                log.info("✅ Collection '{}' already exists", collectionName);
            }

        } catch (Exception e) {
            log.error("❌ Failed to initialize Qdrant collection: {}", e.getMessage(), e);
            // No lanzamos excepción para no detener el inicio de la aplicación
            // La creación de embeddings fallará con un error más claro
        }
    }

    private boolean collectionExists() throws ExecutionException, InterruptedException {
        try {
            Collections.CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName)
                    .get();
            return info != null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof io.grpc.StatusRuntimeException) {
                io.grpc.StatusRuntimeException grpcEx = (io.grpc.StatusRuntimeException) e.getCause();
                if (grpcEx.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                    return false;
                }
            }
            throw e;
        }
    }

    private void createCollection() throws ExecutionException, InterruptedException {
        // Mapear el tipo de distancia
        Collections.Distance distanceType;
        switch (distance.toUpperCase()) {
            case "COSINE":
                distanceType = Collections.Distance.Cosine;
                break;
            case "EUCLID":
            case "EUCLIDEAN":
                distanceType = Collections.Distance.Euclid;
                break;
            case "DOT":
                distanceType = Collections.Distance.Dot;
                break;
            default:
                distanceType = Collections.Distance.Cosine;
        }

        // Crear configuración de la colección
        Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                .setSize(dimension)
                .setDistance(distanceType)
                .build();

        // Crear la colección
        qdrantClient.createCollectionAsync(
                collectionName,
                vectorParams
        ).get();
    }
}
