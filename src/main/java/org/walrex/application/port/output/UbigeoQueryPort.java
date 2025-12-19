package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.application.dto.response.DistrictResponse;
import org.walrex.application.dto.response.UbigeoRecord;

import java.util.Map;
import java.util.Set;

public interface UbigeoQueryPort {

    Uni<Set<String>> existingDepartmentCodes();

    Uni<Set<String>> existingProvinceCodes();

    Uni<Set<String>> existingDistrictCodes();

    /**
     * Busca un distrito por su código de ubigeo.
     *
     * Retorna el distrito con toda su jerarquía (provincia y departamento) si existe.
     * Si no existe, retorna un Uni vacío (null).
     *
     * @param codeUbigeo Código de ubigeo del distrito (6 dígitos)
     * @return Uni con DistrictResponse o null si no existe
     */
    Uni<DistrictResponse> findDistrictByCode(String codeUbigeo);

}
