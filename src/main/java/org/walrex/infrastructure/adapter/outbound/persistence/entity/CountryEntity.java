package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "country", uniqueConstraints = {
        @UniqueConstraint(name = "code_phone_uk", columnNames = {"code_phone_iso"}),
        @UniqueConstraint(name = "country_code2_unique", columnNames = {"code_iso2"}),
        @UniqueConstraint(name = "country_code3_unique", columnNames = {"code_iso3"}),
        @UniqueConstraint(name = "country_name_unique", columnNames = {"name_iso"}),
        @UniqueConstraint(name = "id_country_pk", columnNames = {"id"})
})
public class CountryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code_iso2")
    private String alphabeticCode2;

    @Column(name = "code_iso3")
    private String alphabeticCode3;

    @Column(name = "numeric_code")
    private Integer numericCode;

    @Column(name = "name_iso")
    private String name;

    @Column(name = "code_phone_iso")
    private String phoneCode;

    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "unicode_flag")
    private String unicodeFlag;
}
