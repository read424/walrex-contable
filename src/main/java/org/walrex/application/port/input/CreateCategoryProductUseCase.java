package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.CategoryProduct;

public interface CreateCategoryProductUseCase {

    /**
     * Crea una nueva categoria de producto en el sistema.
     *
     * @param categoryProduct Datos necesarios para crear categoria de producto
     * @return Uni con la categoria de producto creada
     * @throws com.walrex.domain.exception.DuplicateCategoryProductException
     *         si ya existe una categoria de producto con los mismos datos únicos
     * @throws com.walrex.domain.exception.InvalidCategoryProductDataException
     *         si los datos no cumplen las reglas de negocio
     */
    Uni<CategoryProduct> agregarCategory(CategoryProduct categoryProduct);
}
