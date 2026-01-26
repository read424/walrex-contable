package org.walrex.infrastructure.adapter.outbound.persistence.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "users_username_unique", columnNames = {"username"})
})
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_client", nullable = false)
    private Integer clientId;

    @Column(nullable = false)
    private String username;

    @Column(name = "username_type", nullable = false)
    private String usernameType;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Builder.Default
    @Column(name = "pin_attempts", nullable = false)
    private Integer pinAttempts = 0;

    @Column(name = "pin_locked_until")
    private OffsetDateTime pinLockedUntil;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Integer active = 1;

    @Builder.Default
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_type")
    private String mfaType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
