package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.ProductType;

/**
 * Conversor JPA para ProductType enum.
 *
 * Convierte el enum ProductType a/desde String para la base de datos.
 * La base de datos usa valores lowercase (storable, consumable, service).
 */
@Converter(autoApply = true)
public class ProductTypeConverter implements AttributeConverter<ProductType, String> {

    @Override
    public String convertToDatabaseColumn(ProductType attribute) {
        if (attribute == null) {
            return ProductType.STORABLE.getValue(); // Default
        }
        return attribute.getValue();
    }

    @Override
    public ProductType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return ProductType.STORABLE; // Default
        }
        return ProductType.fromValue(dbData);
    }
}
