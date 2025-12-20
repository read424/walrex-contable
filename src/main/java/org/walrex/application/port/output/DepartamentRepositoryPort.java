package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Departament;

public interface DepartamentRepositoryPort {

    /**
     * Persiste un nuevo departamento.
     *
     * @param departament Entidad de dominio a persistir
     * @return Uni con el departamento persistido
     */
    Uni<Departament> save(Departament departament);

    /**
     * Actualiza un departamento existente.
     *
     * @param departament Entidad de dominio con los datos actualizados
     * @return Uni con el departamento actualizado
     */
    Uni<Departament> update(Departament departament);

    /**
     * Elimina lógicamente un departamento (soft delete).
     *
     * @param id Identificador del departamento
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> softDelete(Integer id);

    /**
     * Elimina físicamente un departamento (hard delete).
     *
     * @param id Identificador del departamento
     * @return Uni<Boolean> true si se eliminó, false si no existía
     */
    Uni<Boolean> hardDelete(Integer id);

    /**
     * Restaura un departamento previamente eliminado.
     *
     * @param id Identificador del departamento
     * @return Uni<Boolean> true si se restauró, false si no existía
     */
    Uni<Boolean> restore(Integer id);
}
