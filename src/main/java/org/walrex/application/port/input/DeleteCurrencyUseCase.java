package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteCurrencyUseCase {

    /**
     * Elimina lógicamente una moneda (soft delete).
     *
     * La moneda no se borra físicamente, se marca como eliminada
     * estableciendo la fecha de eliminación.
     *
     * @param id Identificador de la moneda a eliminar
     * @return Uni<Void> que completa cuando la operación termina
     * @throws com.walrex.domain.exception.CurrencyNotFoundException
     *         si no existe una moneda activa con el ID proporcionado
     */
    Uni<Boolean> execute(Integer id);

    /**
     * Restaura una moneda previamente eliminada.
     *
     * @param id Identificador de la moneda a restaurar
     * @return Uni<Void> que completa cuando la operación termina
     * @throws com.walrex.domain.exception.CurrencyNotFoundException
     *         si no existe una moneda eliminada con el ID proporcionado
     */
    Uni<Boolean> restore(Integer id);
}
