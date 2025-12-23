package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.CreateCategoryProductUseCase;
import org.walrex.application.port.input.ListCategoryProductUseCase;
import org.walrex.application.port.output.CategoryProductRepositoryPort;
import org.walrex.domain.model.CategoryProduct;
import org.walrex.domain.model.CategoryProductWithChildren;

import java.util.List;

@Slf4j
@ApplicationScoped
public class CategoryProductService implements CreateCategoryProductUseCase, ListCategoryProductUseCase {

    @Inject
    CategoryProductRepositoryPort categoryProductRepositoryPort;


    @Override
    public Uni<CategoryProduct> agregarCategory(CategoryProduct categoryProduct) {
        return categoryProductRepositoryPort.addCategory(categoryProduct);
    }

    @Override
    public Uni<List<CategoryProductWithChildren>> listCategoriesByParentId(Integer parentId) {
        return categoryProductRepositoryPort.findByParentId(parentId);
    }
}
