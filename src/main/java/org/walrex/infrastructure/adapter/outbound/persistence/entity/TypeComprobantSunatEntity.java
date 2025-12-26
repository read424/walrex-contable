package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity for Type Comprobant SUNAT (document_types table).
 * Represents fiscal document types from SUNAT.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "document_types")
public class TypeComprobantSunatEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sunat_code", nullable = false, unique = true, length = 3)
    private String sunatCode;

    @Column(name = "name_document", nullable = false, length = 255)
    private String nameDocument;
}
