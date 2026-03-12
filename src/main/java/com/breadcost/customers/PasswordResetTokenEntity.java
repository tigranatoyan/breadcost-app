package com.breadcost.customers;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetTokenEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, unique = true)
    private String token;

    /** Expiry stored as epoch millis to avoid H2 timestamp timezone issues. */
    @Column(nullable = false)
    private long expiresAtEpochMs;

    @Builder.Default
    private boolean used = false;

    @Builder.Default
    @Column(nullable = false)
    private long createdAtEpochMs = System.currentTimeMillis();
}
