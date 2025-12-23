package org.walrex.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.walrex.application.port.input.CreateProductBrandUseCase;
import org.walrex.application.port.input.ListProductBrandUseCase;
import org.walrex.application.port.output.ProductBrandRepositoryPort;
import org.walrex.domain.model.ProductBrand;

import java.util.List;

@Slf4j
@ApplicationScoped
public class ProductBrandService implements CreateProductBrandUseCase, ListProductBrandUseCase {

    @Inject
    ProductBrandRepositoryPort productBrandRepositoryPort;

    @Override
    public Uni<ProductBrand> createProductBrand(ProductBrand productBrand) {
        return productBrandRepositoryPort.save(productBrand);
    }

    @Override
    public Uni<List<ProductBrand>> listProductBrands() {
        return productBrandRepositoryPort.findAll();
    }
}
