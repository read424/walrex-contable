package org.walrex.application.port.output;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductBrand;

import java.util.List;

public interface ProductBrandRepositoryPort {

    Uni<ProductBrand> save(ProductBrand productBrand);

    Uni<List<ProductBrand>> findAll();
}
