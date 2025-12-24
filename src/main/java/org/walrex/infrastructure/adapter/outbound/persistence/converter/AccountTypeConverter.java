package org.walrex.infrastructure.adapter.outbound.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.walrex.domain.model.AccountType;

/**
 * Convertidor JPA para mapear el tipo ENUM de PostgreSQL account_type
 * al enum Java AccountType.
 *
 * Este convertidor es necesario para trabajar con tipos ENUM nativos de PostgreSQL
 * en un contexto reactivo con Hibernate Reactive.
 */
@Converter(autoApply = true)
public class AccountTypeConverter implements AttributeConverter<AccountType, String> {

    @Override
    public String convertToDatabaseColumn(AccountType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public AccountType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return AccountType.valueOf(dbData.toUpperCase());
    }
}
