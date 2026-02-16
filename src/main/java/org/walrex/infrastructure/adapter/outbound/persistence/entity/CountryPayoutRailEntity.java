package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "country_payout_rail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryPayoutRailEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "country_id", nullable = false)
    public Integer countryId;

    @Column(name = "payout_rail_id", nullable = false)
    public Integer payoutRailId;

    @Column(length = 1)
    @Builder.Default
    public String status = "1";
}
