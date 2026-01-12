package org.walrex.domain.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Tipo de visualización de un atributo de producto.
 *
 * Define cómo se debe mostrar el atributo en la interfaz de usuario:
 * - SELECT: Desplegable de selección única
 * - RADIO: Botones de radio
 * - COLOR: Selector de color visual
 * - TEXT: Campo de texto libre
 */
@Getter
public enum AttributeDisplayType {
    SELECT("select"),
    RADIO("radio"),
    COLOR("color"),
    TEXT("text");

    @JsonValue
    private final String value;

    AttributeDisplayType(String value) {
        this.value = value;
    }

    /**
     * Convierte una cadena de texto a un AttributeDisplayType.
     *
     * @param value Valor en formato string (case-insensitive)
     * @return El enum correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static AttributeDisplayType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.toLowerCase().trim();
        for (AttributeDisplayType type : AttributeDisplayType.values()) {
            if (type.value.equals(normalizedValue)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid AttributeDisplayType: " + value +
                ". Valid values are: select, radio, color, text");
    }
}
