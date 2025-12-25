package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.AccountingBookType;

/**
 * JPA Converter for mapping PostgreSQL enum 'accounting_book_type' to Java enum.
 */
@Converter(autoApply = true)
public class AccountingBookTypeConverter implements AttributeConverter<AccountingBookType, String> {

    @Override
    public String convertToDatabaseColumn(AccountingBookType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public AccountingBookType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return AccountingBookType.fromString(dbData);
    }
}
