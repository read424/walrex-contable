package org.walrex.infrastructure.adapter.outbound.persistence;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.walrex.application.port.output.ProductBrandRepositoryPort;
import org.walrex.domain.exception.DuplicateProductBrandException;
import org.walrex.domain.model.ProductBrand;
import org.walrex.infrastructure.adapter.outbound.persistence.mapper.ProductBrandMapper;
import org.walrex.infrastructure.adapter.outbound.persistence.repository.ProductBrandRepository;

import java.util.List;

@Slf4j
@ApplicationScoped
public class ProductBrandPersistenceAdapter implements ProductBrandRepositoryPort {

    @Inject
    ProductBrandRepository productBrandRepository;

    @Inject
    ProductBrandMapper productBrandMapper;

    @Override
    public Uni<ProductBrand> save(ProductBrand productBrand) {
        return productBrandRepository.persist(productBrandMapper.toEntity(productBrand))
                .onItem().transform(productBrandMapper::toDomain)
                .onFailure(this::isDuplicateKeyException)
                .transform(throwable -> new DuplicateProductBrandException(
                        "Ya existe una marca de producto con el nombre: " + productBrand.name()
                ));
    }

    @Override
    public Uni<List<ProductBrand>> findAll() {
        return productBrandRepository.findAllOrdered()
                .onItem().transform(entities ->
                        entities.stream()
                                .map(productBrandMapper::toDomain)
                                .toList()
                );
    }

    private boolean isDuplicateKeyException(Throwable throwable) {
        if (throwable instanceof ConstraintViolationException) {
            String constraintName = ((ConstraintViolationException) throwable).getConstraintName();
            return constraintName != null && constraintName.contains("product_brand_name_uk");
        }
        return throwable.getCause() instanceof ConstraintViolationException &&
                isDuplicateKeyException(throwable.getCause());
    }
}
