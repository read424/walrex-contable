package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.District;
import org.walrex.domain.model.Province;

public interface UbigeoPersistencePort {

    /**
     * Busca un departamento por su código.
     * @param codigo Código del departamento (2 dígitos)
     * @return Uni con el departamento o null si no existe
     */
    Uni<Departament> findDepartmentByCode(String codigo);

    /**
     * Busca una provincia por su código.
     * @param codigo Código de la provincia (4 dígitos)
     * @return Uni con la provincia o null si no existe
     */
    Uni<Province> findProvinceByCode(String codigo);

    /**
     * Busca un distrito por su código.
     * @param codigo Código del distrito (6 dígitos - ubigeo completo)
     * @return Uni con el distrito o null si no existe
     */
    Uni<District> findDistrictByCodeDomain(String codigo);

    /**
     * Persiste un nuevo departamento.
     * @param departament Departamento a persistir
     * @return Uni con el departamento persistido
     */
    Uni<Departament> saveDepartment(Departament departament);

    /**
     * Persiste una nueva provincia.
     * @param province Provincia a persistir
     * @return Uni con la provincia persistida
     */
    Uni<Province> saveProvince(Province province);

    /**
     * Persiste un nuevo distrito.
     * @param district Distrito a persistir
     * @return Uni con el distrito persistido
     */
    Uni<District> saveDistrict(District district);
}
