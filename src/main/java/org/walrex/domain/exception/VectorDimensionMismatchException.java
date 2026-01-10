package org.walrex.domain.exception;

/**
 * Excepción crítica lanzada cuando hay un desajuste de dimensiones entre el modelo
 * de embeddings y la configuración de Qdrant.
 *
 * Esta es una excepción NO RECUPERABLE que debe detener inmediatamente el proceso
 * de sincronización para evitar consumo innecesario de APIs y costos.
 */
public class VectorDimensionMismatchException extends RuntimeException {

    private final Integer expectedDimension;
    private final Integer actualDimension;

    public VectorDimensionMismatchException(String message, Integer expectedDimension, Integer actualDimension) {
        super(String.format(
            "CONFIGURACIÓN CRÍTICA: %s | Esperado: %d dimensiones, Obtenido: %d dimensiones. " +
            "Verifique la configuración de 'qdrant.dimension' en application.yml y reinicie la colección de Qdrant.",
            message, expectedDimension, actualDimension
        ));
        this.expectedDimension = expectedDimension;
        this.actualDimension = actualDimension;
    }

    public VectorDimensionMismatchException(String message, Throwable cause) {
        super(message, cause);
        this.expectedDimension = null;
        this.actualDimension = null;
    }

    public Integer getExpectedDimension() {
        return expectedDimension;
    }

    public Integer getActualDimension() {
        return actualDimension;
    }

    /**
     * Indica si esta excepción es crítica y debe detener el proceso completo.
     * Siempre retorna true para VectorDimensionMismatchException.
     */
    public boolean isCritical() {
        return true;
    }
}
