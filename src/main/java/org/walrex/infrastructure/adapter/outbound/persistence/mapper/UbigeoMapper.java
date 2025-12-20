package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.Departament;
import org.walrex.domain.model.District;
import org.walrex.domain.model.Province;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DepartamentEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.DistrictEntity;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProvinceEntity;

import java.util.List;

/**
 * Mapper entre los modelos de dominio de Ubigeo (Departament, Province, District)
 * y sus entidades de persistencia (DepartamentEntity, ProvinceEntity, DistrictEntity).
 *
 * Usa MapStruct para generar automáticamente el código de mapeo en tiempo de compilación.
 *
 * Siguiendo el patrón hexagonal, este mapper pertenece a la capa de infraestructura
 * ya que conoce tanto el modelo de dominio como los detalles de persistencia.
 *
 * Configuración:
 * - componentModel = "cdi": Integración con CDI de Quarkus (permite @Inject)
 * - unmappedTargetPolicy = IGNORE: Ignora campos no mapeados
 * - injectionStrategy = CONSTRUCTOR: Usa inyección por constructor
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface UbigeoMapper {

    // ==================== Departament Mappings ====================

    /**
     * Convierte una entidad de dominio Departament a DepartamentEntity para persistencia.
     *
     * Mapeos:
     * - code → codigo
     * - name → nombre
     * - created_at → createdAt
     * - updated_at → udpdatedAt (nota: typo en la entidad)
     */
    @Mapping(target = "codigo", source = "code")
    @Mapping(target = "nombre", source = "name")
    @Mapping(target = "createdAt", source = "created_at")
    @Mapping(target = "udpdatedAt", source = "updated_at")
    DepartamentEntity toEntity(Departament domain);

    /**
     * Convierte una entidad de persistencia DepartamentEntity a modelo de dominio Departament.
     *
     * Mapeos:
     * - codigo → code
     * - nombre → name
     * - createdAt → created_at
     * - udpdatedAt → updated_at
     */
    @Mapping(target = "code", source = "codigo")
    @Mapping(target = "name", source = "nombre")
    @Mapping(target = "created_at", source = "createdAt")
    @Mapping(target = "updated_at", source = "udpdatedAt")
    Departament toDepartamentDomain(DepartamentEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     */
    List<Departament> toDepartamentDomainList(List<DepartamentEntity> entities);

    // ==================== Province Mappings ====================

    /**
     * Convierte una entidad de dominio Province a ProvinceEntity para persistencia.
     *
     * Mapeos:
     * - code → codigo
     * - created_at → createdAt
     * - updated_at → updatedAt
     * - departament → departament (recursivo, usa toEntity)
     */
    @Mapping(target = "codigo", source = "code")
    @Mapping(target = "createdAt", source = "created_at")
    @Mapping(target = "updatedAt", source = "updated_at")
    @Mapping(target = "departament", source = "departament")
    ProvinceEntity toEntity(Province domain);

    /**
     * Convierte una entidad de persistencia ProvinceEntity a modelo de dominio Province.
     *
     * Mapeos:
     * - codigo → code
     * - createdAt → created_at
     * - updatedAt → updated_at
     * - departament → departament (recursivo, usa toDepartamentDomain)
     */
    @Mapping(target = "code", source = "codigo")
    @Mapping(target = "created_at", source = "createdAt")
    @Mapping(target = "updated_at", source = "updatedAt")
    @Mapping(target = "departament", source = "departament")
    Province toProvinceDomain(ProvinceEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     */
    List<Province> toProvinceDomainList(List<ProvinceEntity> entities);

    // ==================== District Mappings ====================

    /**
     * Convierte una entidad de dominio District a DistrictEntity para persistencia.
     *
     * Mapeos:
     * - code → codigo
     * - created_at → createdAt
     * - updated_at → updatedAt
     * - province → province (recursivo, usa toEntity para Province)
     */
    @Mapping(target = "codigo", source = "code")
    @Mapping(target = "createdAt", source = "created_at")
    @Mapping(target = "updatedAt", source = "updated_at")
    @Mapping(target = "province", source = "province")
    DistrictEntity toEntity(District domain);

    /**
     * Convierte una entidad de persistencia DistrictEntity a modelo de dominio District.
     *
     * Mapeos:
     * - codigo → code
     * - createdAt → created_at
     * - updatedAt → updated_at
     * - province → province (recursivo, usa toProvinceDomain)
     */
    @Mapping(target = "code", source = "codigo")
    @Mapping(target = "created_at", source = "createdAt")
    @Mapping(target = "updated_at", source = "updatedAt")
    @Mapping(target = "province", source = "province")
    District toDistrictDomain(DistrictEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     */
    List<District> toDistrictDomainList(List<DistrictEntity> entities);
}
