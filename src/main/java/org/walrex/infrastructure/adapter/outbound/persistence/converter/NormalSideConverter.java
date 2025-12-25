package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.NormalSide;

/**
 * Convertidor JPA para mapear el tipo ENUM de PostgreSQL normal_side
 * al enum Java NormalSide.
 *
 * Este convertidor es necesario para trabajar con tipos ENUM nativos de PostgreSQL
 * en un contexto reactivo con Hibernate Reactive.
 */
@Converter(autoApply = true)
public class NormalSideConverter implements AttributeConverter<NormalSide, String> {

    @Override
    public String convertToDatabaseColumn(NormalSide attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public NormalSide convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return NormalSide.valueOf(dbData.toUpperCase());
    }
}
