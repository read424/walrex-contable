package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.CategoryProductWithChildren;

import java.util.List;

public interface ListCategoryProductUseCase {

    /**
     * Lista categorías de productos filtradas por parentId.
     *
     * @param parentId ID de la categoría padre. Si es null, devuelve las categorías raíz (sin padre)
     * @return Uni con lista de categorías de producto con información de hijos
     */
    Uni<List<CategoryProductWithChildren>> listCategoriesByParentId(Integer parentId);
}
