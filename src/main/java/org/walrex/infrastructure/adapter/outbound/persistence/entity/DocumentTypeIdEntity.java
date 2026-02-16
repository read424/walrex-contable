package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad para la tabla type_document_id
 */
@Entity
@Table(name = "type_document_id")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "country")
public class DocumentTypeIdEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sigla", length = 7, nullable = false, unique = true)
    private String sigla;

    @Column(name = "det_name", length = 50, nullable = false, unique = true)
    private String detName;

    @Builder.Default
    @Column(name = "status", length = 1, nullable = false)
    private String status = "1";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", nullable = false)
    private CountryEntity country;
}
