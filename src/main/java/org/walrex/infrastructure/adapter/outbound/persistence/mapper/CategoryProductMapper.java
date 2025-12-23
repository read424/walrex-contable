package org.walrex.infrastructure.adapter.outbound.persistence.mapper;

import org.mapstruct.*;
import org.walrex.domain.model.CategoryProduct;
import org.walrex.domain.model.CategoryProductWithChildren;
import org.walrex.infrastructure.adapter.outbound.persistence.entity.CategoryProductEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CategoryProductMapper {

    /**
     * Convierte una entidad de dominio a una entidad de persistencia.
     *
     * MapStruct mapea automáticamente todos los campos porque tienen el mismo nombre:
     * - id → id
     * - name → name
     * - details → details
     * - createdAt → createdAt
     *
     * El parentId se maneja mediante @AfterMapping para establecer la relación parent
     *
     * @param domain Modelo de dominio CategoryProduct
     * @return CategoryProductEntity para persistencia
     */
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @BeanMapping(qualifiedByName = "linkParentLogic")
    CategoryProductEntity toEntity(CategoryProduct domain);


    /**
     * Establece la relación parent usando el parentId del dominio.
     * Este método se ejecuta después de toEntity() porque coincide con la firma:
     * - MappingTarget: CategoryProductEntity (el resultado de toEntity)
     * - Source: CategoryProduct (el parámetro de toEntity)
     */
    @AfterMapping
    @Named("linkParentLogic")
    default void linkParent(CategoryProduct domain, @MappingTarget CategoryProductEntity entity) {
        if (domain.parentId() != null) {
            CategoryProductEntity parent = new CategoryProductEntity();
            parent.setId(domain.parentId());
            entity.setParent(parent);
        }
    }
    /**
     * Convierte una entidad de persistencia a una entidad de dominio.
     *
     * El parentId se extrae del objeto parent si existe
     */
    @Mapping(target = "parentId", expression = "java(entity.getParent() != null ? entity.getParent().getId() : null)")
    CategoryProduct toDomain(CategoryProductEntity entity);

    /**
     * Convierte una entidad de persistencia a CategoryProductWithChildren.
     *
     * Calcula hasChildren y childrenCount desde la lista de children
     */
    @Mapping(target = "parentId", expression = "java(entity.getParent() != null ? entity.getParent().getId() : null)")
    @Mapping(target = "hasChildren", expression = "java(entity.getChildren() != null && !entity.getChildren().isEmpty())")
    @Mapping(target = "childrenCount", expression = "java(entity.getChildren() != null ? entity.getChildren().size() : 0)")
    CategoryProductWithChildren toWithChildren(CategoryProductEntity entity);
}
