package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;

public interface DeleteProvinceUseCase {
    /**
     * Elimina lógicamente una provincia (soft delete).
     *
     * La provincia no se borra físicamente, se marca como eliminada
     * estableciendo status = false.
     *
     * @param id Identificador de la provincia a eliminar
     * @return Uni<Void> que completa cuando la operación termina
     * @throws org.walrex.domain.exception.ProvinceNotFoundException
     *         si no existe una provincia activa con el ID proporcionado
     */
    Uni<Void> deshabilitar(Integer id);
}
