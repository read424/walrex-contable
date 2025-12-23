package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.output.CategoryProductRepositoryPort;
import org.walrex.domain.model.CategoryProduct;
import org.walrex.domain.model.CategoryProductWithChildren;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.CategoryProductMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.CategoryProductRepository;

import java.util.List;

@Slf4j
@ApplicationScoped
public class CategoryProductPersistenceAdapter implements CategoryProductRepositoryPort {

    @Inject
    CategoryProductRepository categoryProductRepository;

    @Inject
    CategoryProductMapper categoryProductMapper;

    @Override
    public Uni<CategoryProduct> addCategory(CategoryProduct categoryProduct) {
        return categoryProductRepository.persist(categoryProductMapper.toEntity(categoryProduct))
                .onItem().transform(categoryProductMapper::toDomain);
    }

    @Override
    public Uni<List<CategoryProductWithChildren>> findByParentId(Integer parentId) {
        Uni<List<org.walrex.infrastructure.adapter.outbound.persistence.entity.CategoryProductEntity>> entitiesUni;

        if (parentId == null) {
            entitiesUni = categoryProductRepository.findRootCategories();
        } else {
            entitiesUni = categoryProductRepository.findByParentId(parentId);
        }

        return entitiesUni.onItem().transform(entities ->
            entities.stream()
                .map(categoryProductMapper::toWithChildren)
                .toList()
        );
    }
}
