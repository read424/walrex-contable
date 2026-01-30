package org.walrex.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class User {

    private Integer id;

    /** FK lógica hacia Customer */
    private Integer customerId;

    /** Email o teléfono */
    private String username;

    /** PHONE | EMAIL */
    private String usernameType;

    /** Hash del PIN */
    private String pinHash;

    private Boolean biometricEnabled;

    private String biometricType;

    private java.time.OffsetDateTime biometricEnrolledAt;

    /** Seguridad */
    private int pinAttempts;
    private OffsetDateTime pinLockedUntil;

    /** Estado */
    private Integer active;

    /** MFA / biometría (futuro) */
    private boolean mfaEnabled;

    private String mfaType;
}
