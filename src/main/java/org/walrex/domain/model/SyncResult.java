package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de una operación de sincronización de embeddings de cuentas contables.
 * Contiene estadísticas y detalles del proceso de sincronización.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /**
     * Total de cuentas procesadas en esta sincronización.
     */
    private Integer totalProcessed;

    /**
     * Cuentas sincronizadas exitosamente.
     */
    private Integer successfulSyncs;

    /**
     * Cuentas que fallaron durante la sincronización.
     */
    private Integer failedSyncs;

    /**
     * Cuentas omitidas (ya estaban sincronizadas o inactivas).
     */
    private Integer skippedAccounts;

    /**
     * Fecha y hora de inicio de la sincronización.
     */
    private OffsetDateTime startedAt;

    /**
     * Fecha y hora de finalización de la sincronización.
     */
    private OffsetDateTime completedAt;

    /**
     * Duración total del proceso en milisegundos.
     */
    private Long durationMs;

    /**
     * Lista de IDs de cuentas que fallaron durante la sincronización.
     */
    @Builder.Default
    private List<Integer> failedAccountIds = new ArrayList<>();

    /**
     * Mensajes de error si los hubo.
     */
    @Builder.Default
    private List<String> errorMessages = new ArrayList<>();

    /**
     * Indica si la sincronización completó sin errores críticos.
     */
    private Boolean successful;

    /**
     * Calcula el porcentaje de éxito de la sincronización.
     *
     * @return Porcentaje de cuentas sincronizadas exitosamente
     */
    public double getSuccessRate() {
        if (totalProcessed == null || totalProcessed == 0) {
            return 0.0;
        }
        return (successfulSyncs.doubleValue() / totalProcessed.doubleValue()) * 100;
    }

    /**
     * Agrega un error a la lista de mensajes de error.
     */
    public void addError(Integer accountId, String errorMessage) {
        if (failedAccountIds == null) {
            failedAccountIds = new ArrayList<>();
        }
        if (errorMessages == null) {
            errorMessages = new ArrayList<>();
        }
        failedAccountIds.add(accountId);
        errorMessages.add(String.format("Account %d: %s", accountId, errorMessage));
    }
}
