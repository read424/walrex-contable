package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "institution_payout_rail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class InstitutionPayoutRailEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_id", nullable = false)
    private BankEntity bank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payout_rail_id", nullable = false)
    private PayoutRailEntity payoutRail;

    @Builder.Default
    @Column(name = "status", length = 1, nullable = false)
    private String status = "1";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "required_fields", columnDefinition = "jsonb")
    private String requiredFields;
}
