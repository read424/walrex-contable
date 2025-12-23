package org.walrex.infrastructure.adapter.inbound.mapper;

import org.mapstruct.*;
import org.walrex.application.dto.request.CreateCategoryProductRequest;
import org.walrex.application.dto.response.CategoryProductResponse;
import org.walrex.application.dto.response.CategoryProductSelectResponse;
import org.walrex.domain.model.CategoryProduct;
import org.walrex.domain.model.CategoryProductWithChildren;

@Mapper(
        componentModel = MappingConstants.ComponentModel.CDI,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CategoryProductMapper {

    /**
     * Convierte el modelo de dominio a DTO de respuesta.
     *
     * Mapeos especiales:
     * - deletedAt: Se ignora (no se expone al exterior)
     *
     * @param categoryProduct Modelo de dominio
     * @return CategoryProductResponse DTO de respuesta
     */
    CategoryProductResponse toResponse(CategoryProduct categoryProduct);

    CategoryProduct toDomain(CreateCategoryProductRequest request);

    /**
     * Convierte CategoryProductWithChildren a CategoryProductSelectResponse.
     *
     * Mapea details del dominio a description en el response
     */
    @Mapping(source = "details", target = "description")
    CategoryProductSelectResponse toSelectResponse(CategoryProductWithChildren categoryProductWithChildren);
}
