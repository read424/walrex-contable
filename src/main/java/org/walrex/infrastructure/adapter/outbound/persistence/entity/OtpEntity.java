package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.walrex.domain.model.OtpPurpose;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "otp",
    indexes = {
        @Index(
            name = "idx_otp_reference_purpose",
            columnList = "reference_id, purpose"
        ),
        @Index(
            name = "idx_otp_target_active",
            columnList = "target, used, expires_at"
        )
    }
    )
public class OtpEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", length = 64, nullable = false, unique = true)
    public String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 32, nullable = false)
    private OtpPurpose purpose;

    @Column(length = 128, nullable = false)
    public String target;

    @Column(name = "otp_hash", length = 128, nullable = false)
    public String otpHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean used;
}
