package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteCountryUseCase {
    /**
     * Elimina lógicamente un pais (soft delete).
     *
     * El paś no se borra físicamente, se marca como eliminada
     * estableciendo la fecha de eliminación.
     *
     * @param id Identificador del pais a eliminar
     * @return Uni<Void> que completa cuando la operación termina
     * @throws com.walrex.domain.exception.CountryNotFoundException
     *         si no existe un pais activo con el ID proporcionado
     */
    Uni<Boolean> deshabilitar(Integer id);

    /**
     * Restaura un pais previamente eliminada.
     *
     * @param id Identificador del pais a restaurar
     * @return Uni<Void> que completa cuando la operación termina
     * @throws com.walrex.domain.exception.CountryNotFoundException
     *         si no existe un pais eliminada con el ID proporcionado
     */
    Uni<Boolean> habilitar(Integer id);
}
