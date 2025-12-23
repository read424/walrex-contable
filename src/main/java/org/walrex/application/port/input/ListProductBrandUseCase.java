package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductBrand;

import java.util.List;

public interface ListProductBrandUseCase {

    /**
     * Lista todas las marcas de producto en el sistema.
     *
     * @return Uni con lista de marcas de producto
     */
    Uni<List<ProductBrand>> listProductBrands();
}
