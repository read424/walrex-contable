package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.walrex.domain.model.AccountingAccount;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.AccountingAccountEntity;

import java.util.List;

/**
 * Mapper entre el modelo de dominio AccountingAccount y la entidad de persistencia AccountingAccountEntity.
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
 *
 * Nota: Como todos los campos tienen el mismo nombre en AccountingAccount y AccountingAccountEntity,
 * MapStruct puede hacer el mapeo automáticamente sin necesidad de @Mapping.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface AccountingAccountMapper {

    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - code → code
     * - name → name
     * - type → type
     * - normalSide → normalSide
     * - active → active
     * - createdAt → createdAt
     * - updatedAt → updatedAt
     * - deletedAt → deletedAt
     *
     * @param domain Modelo de dominio AccountingAccount
     * @return AccountingAccountEntity para persistencia
     */
    AccountingAccountEntity toEntity(AccountingAccount domain);

    /**
     * Convierte una entidad de persistencia a un modelo de dominio.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre.
     *
     * @param entity AccountingAccountEntity de persistencia
     * @return AccountingAccount modelo de dominio
     */
    AccountingAccount toDomain(AccountingAccountEntity entity);

    /**
     * Convierte una lista de entidades de persistencia a lista de modelos de dominio.
     *
     * @param entities Lista de AccountingAccountEntity
     * @return Lista de AccountingAccount
     */
    List<AccountingAccount> toDomainList(List<AccountingAccountEntity> entities);

    /**
     * Convierte una lista de modelos de dominio a lista de entidades de persistencia.
     *
     * @param domains Lista de AccountingAccount
     * @return Lista de AccountingAccountEntity
     */
    List<AccountingAccountEntity> toEntityList(List<AccountingAccount> domains);
}
