package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Province;

public interface ProvinceRepositoryPort {
    /**
     * Persiste una nueva provincia.
     *
     * @param province Entidad de dominio a persistir
     * @return Uni con el province persistido
     */
    Uni<Province> save(Province province);

    /**
     * Actualiza una provincia existente.
     *
     * @param province Entidad de dominio con los datos actualizados
     * @return Uni con la provincia actualizado
     */
    Uni<Province> update(Province province);

    /**
     * Elimina lógicamente una provincia (soft delete).
     *
     * @param id Identificador de la provincia
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente una provincia (hard delete).
     *
     * @param id Identificador de la provincia
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura una provincia previamente eliminado.
     *
     * @param id Identificador de la provincia
     * @return Uni<Boolean> true si se restauró, false si no existía
     */
    Uni<Boolean> restore(Integer id);
}
