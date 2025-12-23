package org.walrex.application.port.input;

import io.smallrye.mutiny.Uni;
import org.walrex.domain.model.ProductBrand;

public interface CreateProductBrandUseCase {

    /**
     * Crea una nueva marca de producto en el sistema.
     *
     * @param productBrand Datos necesarios para crear la marca de producto
     * @return Uni con la marca de producto creada
     * @throws org.walrex.domain.exception.DuplicateProductBrandException
     *         si ya existe una marca de producto con el mismo nombre
     */
    Uni<ProductBrand> createProductBrand(ProductBrand productBrand);
}
