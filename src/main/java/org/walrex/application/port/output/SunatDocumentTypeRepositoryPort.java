package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.SunatDocumentType;

/**
 * Puerto de salida para operaciones de escritura en tipos de documentos SUNAT.
 *
 * Define el contrato para persistencia (CQRS - Command side).
 * La implementación se encuentra en la capa de infraestructura (Adapter).
 */
public interface SunatDocumentTypeRepositoryPort {

    /**
     * Persiste un nuevo tipo de documento.
     *
     * @param documentType Entidad de dominio a persistir
     * @return Uni con el tipo de documento persistido (incluye timestamps)
     */
    Uni<SunatDocumentType> save(SunatDocumentType documentType);

    /**
     * Actualiza un tipo de documento existente.
     *
     * @param documentType Entidad de dominio con los datos actualizados
     * @return Uni con el tipo de documento actualizado
     */
    Uni<SunatDocumentType> update(SunatDocumentType documentType);

    /**
     * Desactiva un tipo de documento (marca active=false).
     *
     * @param id Identificador del tipo de documento
     * @return Uni<Boolean> true si se desactivó, false si no existía o ya estaba inactivo
     */
    Uni<Boolean> deactivate(Integer id);

    /**
     * Reactiva un tipo de documento (marca active=true).
     *
     * @param id Identificador del tipo de documento
     * @return Uni<Boolean> true si se reactivó, false si no existía o ya estaba activo
     */
    Uni<Boolean> activate(Integer id);

    /**
     * Elimina físicamente un tipo de documento (hard delete).
     *
     * ⚠️ Usar con precaución. Preferir deactivate().
     *
     * @param id Identificador del tipo de documento
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);
}
