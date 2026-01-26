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

    /** Seguridad */
    private int pinAttempts;
    private OffsetDateTime pinLockedUntil;

    /** Estado */
    private boolean active;

    /** MFA / biometría (futuro) */
    private boolean mfaEnabled;

    private String mfaType;
}
