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
                @Index(name = "idx_otp_reference_purpose", columnList = "referenceId,purpose")
        }
    )
public class OtpEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    public String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    public String target;

    @Column(nullable = false)
    public String otpHash;

    @Column(nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public boolean used;
}
