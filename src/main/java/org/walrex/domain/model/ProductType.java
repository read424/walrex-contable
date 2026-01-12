package org.walrex.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeración para tipos de productos.
 *
 * Define los tres tipos principales de productos que puede manejar el sistema:
 * - STORABLE: Productos almacenables con control completo de inventario
 * - CONSUMABLE: Productos consumibles que se rastrean pero no usan números de serie
 * - SERVICE: Servicios que no tienen propiedades físicas ni control de inventario
 *
 * El valor en base de datos es lowercase para coincidir con el ENUM de PostgreSQL.
 */
public enum ProductType {

    /**
     * Producto almacenable.
     * Permite control completo de inventario, números de serie, propiedades físicas.
     */
    STORABLE("storable"),

    /**
     * Producto consumible.
     * Permite control de inventario pero no números de serie.
     */
    CONSUMABLE("consumable"),

    /**
     * Servicio.
     * No permite control de inventario ni propiedades físicas.
     */
    SERVICE("service");

    private final String value;

    ProductType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Obtiene el enum desde el valor string en base de datos o JSON.
     * Usado por Jackson para deserialización y por conversiones manuales.
     *
     * @param value Valor string (storable, consumable, service)
     * @return ProductType correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    @JsonCreator
    public static ProductType fromValue(String value) {
        if (value == null) {
            return STORABLE; // Default
        }
        for (ProductType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ProductType value: " + value);
    }
}
