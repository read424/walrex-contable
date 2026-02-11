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
@Table(name = "currencies", uniqueConstraints = {
                @UniqueConstraint(name = "uk_currency_numeric_code", columnNames = { "numericcode" }),
                @UniqueConstraint(name = "uk_currency_alphabetic_code", columnNames = { "code_iso3" }),
                @UniqueConstraint(name = "uk_currency_name", columnNames = { "name" })
})
public class CurrencyEntity extends PanacheEntityBase {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "code_iso3")
        private String alphabeticCode;

        @Column(name = "numericcode")
        private Integer numericCode;

        private String name;

        private String symbol;

        private String status;

        @Column(name = "created_at")
        private OffsetDateTime createdAt;

        @Column(name = "updated_at")
        private OffsetDateTime updatedAt;

        @Column(name = "deleted_at")
        private OffsetDateTime deletedAt;
}
