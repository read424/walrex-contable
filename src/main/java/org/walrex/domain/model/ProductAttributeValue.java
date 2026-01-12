package org.walrex.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Modelo de dominio para valores de atributos de producto.
 *
 * Representa un valor específico que puede ser asignado a un atributo de producto.
 * Por ejemplo, si el atributo es "color", los valores podrían ser "rojo", "azul", "verde".
 * Si el atributo es "talla", los valores podrían ser "XS", "S", "M", "L", "XL".
 *
 * El ID es de tipo Integer (auto-generado por la base de datos).
 *
 * Soporta soft delete mediante el campo deletedAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeValue {

    /**
     * Identificador único del valor de atributo (Integer auto-generado)
     * Auto-incrementado por la base de datos
     */
    private Integer id;

    /**
     * ID del atributo al que pertenece este valor
     * Referencia a ProductAttribute.id
     */
    private Integer attributeId;

    /**
     * Nombre descriptivo del valor
     * Máximo 100 caracteres
     * Ej: "Rojo", "Azul", "S", "M", "L"
     */
    private String name;

    /**
     * Color HTML asociado al valor (opcional)
     * Formato: #RRGGBB (6 dígitos hexadecimales)
     * Ej: "#FF0000", "#0000FF"
     * Solo se usa cuando el attributeDisplayType es COLOR
     */
    private String htmlColor;

    /**
     * Secuencia para ordenamiento
     * Permite controlar el orden de visualización
     * Ej: XS=0, S=1, M=2, L=3, XL=4
     */
    private Integer sequence;

    /**
     * Indica si el valor está activo
     * Los valores inactivos no se pueden usar en nuevos productos
     */
    private Boolean active;

    /**
     * Fecha de creación del registro
     */
    private OffsetDateTime createdAt;

    /**
     * Fecha de última actualización del registro
     */
    private OffsetDateTime updatedAt;

    /**
     * Fecha de eliminación lógica del registro
     * Si es null, el registro no ha sido eliminado
     */
    private OffsetDateTime deletedAt;

    /**
     * Verifica si el valor está eliminado (soft delete)
     *
     * @return true si el valor ha sido eliminado lógicamente
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Verifica si el valor está activo y no eliminado
     *
     * @return true si el valor está activo y no ha sido eliminado
     */
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !isDeleted();
    }
}
