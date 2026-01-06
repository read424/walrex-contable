package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.NormalSide;

@Converter(autoApply = true)
public class NormalSideConverter implements AttributeConverter<NormalSide, String> {

    @Override
    public String convertToDatabaseColumn(NormalSide attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public NormalSide convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return NormalSide.valueOf(dbData.toUpperCase());
    }
}