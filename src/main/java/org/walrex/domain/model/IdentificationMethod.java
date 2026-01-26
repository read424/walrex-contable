package org.walrex.domain.model;

public enum IdentificationMethod {
    EMAIL,
    PHONE;

    public static IdentificationMethod fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Identification Method cannot be null");
        }

        try {
            return IdentificationMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid identification method: " + value + ". Valid values are: EMAIL, PHONE"
            );
        }
    }
}
