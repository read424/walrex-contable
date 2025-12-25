package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.EntryStatus;

/**
 * JPA Converter for mapping database VARCHAR to Java enum EntryStatus.
 */
@Converter(autoApply = true)
public class EntryStatusConverter implements AttributeConverter<EntryStatus, String> {

    @Override
    public String convertToDatabaseColumn(EntryStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public EntryStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return EntryStatus.fromString(dbData);
    }
}
