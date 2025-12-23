package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.CategoryProduct;
import org.walrex.domain.model.CategoryProductWithChildren;

import java.util.List;

public interface CategoryProductRepositoryPort {

    Uni<CategoryProduct> addCategory(CategoryProduct categoryProduct);

    Uni<List<CategoryProductWithChildren>> findByParentId(Integer parentId);
}
