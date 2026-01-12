package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.walrex.domain.model.ProductVariant;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.ProductVariantEntity;

/**
 * Mapper MapStruct para transformaciones entre ProductVariant (dominio) y ProductVariantEntity (persistencia).
 *
 * MapStruct genera implementación automática en tiempo de compilación.
 * El patrón hexagonal requiere que las transformaciones entre capas sean explícitas.
 *
 * IMPORTANTE:
 * - La entidad tiene una relación @ManyToOne con ProductTemplateEntity
 * - Solo mapeamos productTemplateId (no el objeto productTemplate completo)
 * - El objeto productTemplate en la entidad se usa solo para lecturas con JOIN FETCH
 */
@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ProductVariantMapper {

    /**
     * Convierte de modelo de dominio a entidad de persistencia.
     *
     * NOTA: El campo 'productTemplate' de la entidad NO se mapea.
     * Solo se mapea productTemplateId porque es el FK en la tabla.
     */
    @Mapping(target = "productTemplate", ignore = true)
    ProductVariantEntity toEntity(ProductVariant domain);

    /**
     * Convierte de entidad de persistencia a modelo de dominio.
     *
     * NOTA: Si la entidad tiene el objeto 'productTemplate' cargado (JOIN FETCH),
     * igualmente solo extraemos el productTemplateId para el dominio.
     * El dominio no necesita el objeto productTemplate completo.
     */
    ProductVariant toDomain(ProductVariantEntity entity);
}
