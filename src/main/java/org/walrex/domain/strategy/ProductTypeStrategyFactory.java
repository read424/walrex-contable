package org.walrex.domain.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.walrex.domain.model.ProductType;

/**
 * Factory para obtener la estrategia adecuada según el tipo de producto.
 *
 * Implementa el patrón Factory para centralizar la creación de estrategias
 * y evitar condicionales dispersos en el código.
 *
 * Las estrategias se inyectan vía CDI para aprovechar:
 * - Inyección de dependencias
 * - Lifecycle management
 * - Testabilidad (se pueden mockear)
 */
@ApplicationScoped
public class ProductTypeStrategyFactory {

    @Inject
    ServiceProductStrategy serviceStrategy;

    @Inject
    StorableProductStrategy storableStrategy;

    @Inject
    ConsumableProductStrategy consumableStrategy;

    /**
     * Obtiene la estrategia correspondiente al tipo de producto.
     *
     * @param type Tipo de producto
     * @return Estrategia específica para ese tipo
     * @throws IllegalArgumentException si el tipo no está soportado
     */
    public ProductTypeStrategy getStrategy(ProductType type) {
        if (type == null) {
            throw new IllegalArgumentException("Product type cannot be null");
        }

        return switch (type) {
            case SERVICE -> serviceStrategy;
            case STORABLE -> storableStrategy;
            case CONSUMABLE -> consumableStrategy;
            // Cuando se agregue un nuevo tipo, el compilador forzará agregarlo aquí
            // default -> throw new IllegalArgumentException("Unsupported product type: " + type);
        };
    }
}
