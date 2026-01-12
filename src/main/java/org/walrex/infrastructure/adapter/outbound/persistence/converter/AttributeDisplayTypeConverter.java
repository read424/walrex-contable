package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.AttributeDisplayType;

/**
 * Conversor JPA para AttributeDisplayType.
 *
 * Convierte entre:
 * - Enum AttributeDisplayType (en código Java)
 * - String lowercase (en base de datos)
 *
 * Valores en DB: "select", "radio", "color", "text"
 * Valores en Enum: SELECT, RADIO, COLOR, TEXT
 *
 * @Converter(autoApply = true) asegura que se aplique automáticamente
 * a todos los campos de tipo AttributeDisplayType.
 */
@Converter(autoApply = true)
public class AttributeDisplayTypeConverter implements AttributeConverter<AttributeDisplayType, String> {

    /**
     * Convierte el enum a su representación en base de datos (lowercase string).
     *
     * @param attribute El valor del enum (puede ser null)
     * @return String en lowercase para almacenar en DB
     */
    @Override
    public String convertToDatabaseColumn(AttributeDisplayType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    /**
     * Convierte el string de la base de datos al enum.
     *
     * @param dbData El valor de la columna en DB (puede ser null)
     * @return El enum correspondiente
     * @throws IllegalArgumentException si el valor en DB no es válido
     */
    @Override
    public AttributeDisplayType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return AttributeDisplayType.fromString(dbData);
    }
}
