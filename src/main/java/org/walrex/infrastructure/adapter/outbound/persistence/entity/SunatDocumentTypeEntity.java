package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entidad JPA para tipos de documentos SUNAT.
 *
 * Representa los tipos de documentos de identidad establecidos por SUNAT:
 * - DNI (01, 1)
 * - RUC (06, 6)
 * - Carné de Extranjería (07, 4)
 * - Pasaporte (11, 7)
 * - etc.
 *
 * Esta tabla se sincroniza periódicamente con los datos oficiales de SUNAT.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sunat_document_types", uniqueConstraints = {
        @UniqueConstraint(name = "sunat_doc_type_code_unique", columnNames = {"code"}),
        @UniqueConstraint(name = "sunat_doc_type_pk", columnNames = {"id"})
})
public class SunatDocumentTypeEntity extends PanacheEntityBase {

    /**
     * Identificador único del tipo de documento.
     * Ejemplo: '01', '06', '07', '11'
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Código SUNAT del documento.
     * Ejemplo: '01', '1', '6', '4', '7'
     * Puede ser diferente al ID para compatibilidad.
     */
    @Column(length = 10, nullable = false, unique = true)
    private String code;

    /**
     * Nombre descriptivo del tipo de documento.
     * Ejemplo: 'DNI', 'RUC', 'Carné de Extranjería'
     */
    @Column(length = 100, nullable = false)
    private String name;

    /**
     * Descripción adicional del tipo de documento.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Longitud exacta del documento.
     * Ejemplo: DNI = 8, RUC = 11
     */
    @Column(name = "length")
    private Integer length;

    /**
     * Patrón regex para validar el formato del documento.
     * Ejemplo: '^[0-9]{8}$' para DNI
     */
    @Column(name = "pattern", length = 50)
    private String pattern;

    /**
     * Indica si el tipo de documento está activo según SUNAT.
     * true = activo, false = descontinuado
     */
    @Column(name = "active")
    private Boolean active;

    /**
     * Timestamp de creación del registro.
     * Se establece automáticamente al crear.
     */
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp de última actualización del registro.
     * Se actualiza automáticamente en cada modificación.
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
