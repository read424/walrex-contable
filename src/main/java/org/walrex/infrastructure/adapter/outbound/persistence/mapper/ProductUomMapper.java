package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.ProductUom;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductUomEntity;

/**
 * Mapper MapStruct para transformaciones entre ProductUom (dominio) y ProductUomEntity (persistencia).
 *
 * MapStruct genera implementación automática en tiempo de compilación.
 * El patrón hexagonal requiere que las transformaciones entre capas sean explícitas.
 *
 * IMPORTANTE:
 * - La entidad tiene una relación @ManyToOne con ProductCategoryUomEntity
 * - Solo mapeamos categoryId (no el objeto category completo)
 * - El objeto category en la entidad se usa solo para lecturas con JOIN FETCH
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ProductUomMapper {

    /**
     * Convierte de modelo de dominio a entidad de persistencia.
     *
     * NOTA: El campo 'category' de la entidad NO se mapea.
     * Solo se mapea categoryId porque es el FK en la tabla.
     */
    @Mapping(target = "category", ignore = true)
    ProductUomEntity toEntity(ProductUom domain);

    /**
     * Convierte de entidad de persistencia a modelo de dominio.
     *
     * NOTA: Si la entidad tiene el objeto 'category' cargado (JOIN FETCH),
     * igualmente solo extraemos el categoryId para el dominio.
     * El dominio no necesita el objeto category completo.
     */
    ProductUom toDomain(ProductUomEntity entity);
}
