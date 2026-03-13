package com.breadcost.unit.service;

import com.breadcost.masterdata.*;
import com.breadcost.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private UserService svc;

    // ── createUser ───────────────────────────────────────────────────────────

    @Test
    void createUser_encodesPasswordAndSaves() {
        when(subscriptionService.getMaxUsers("t1")).thenReturn(10);
        when(userRepo.findByTenantId("t1")).thenReturn(List.of());
        when(passwordEncoder.encode("pass123")).thenReturn("$hashed$");
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = svc.createUser("t1", "alice", "pass123", "Alice", "ADMIN", "dept1");

        assertEquals("t1", user.getTenantId());
        assertEquals("alice", user.getUsername());
        assertEquals("$hashed$", user.getPasswordHash());
        assertEquals("ADMIN", user.getRoles());
        assertTrue(user.isActive());
    }

    @Test
    void createUser_userLimitReached_throws() {
        when(subscriptionService.getMaxUsers("t1")).thenReturn(1);
        var existing = UserEntity.builder().userId("u1").tenantId("t1").active(true).build();
        when(userRepo.findByTenantId("t1")).thenReturn(List.of(existing));

        assertThrows(IllegalStateException.class,
                () -> svc.createUser("t1", "bob", "pass", "Bob", "USER", null));
    }

    @Test
    void createUser_unlimitedPlan_noLimitCheck() {
        // maxUsers=0 means unlimited
        when(subscriptionService.getMaxUsers("t1")).thenReturn(0);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var user = svc.createUser("t1", "bob", "pass", "Bob", "USER", null);

        assertNotNull(user.getUserId());
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    void updateUser_updatesOnlyProvidedFields() {
        var user = UserEntity.builder().userId("u1").tenantId("t1")
                .displayName("Old").roles("USER").active(true).build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.updateUser("t1", "u1", "New Name", null, null, null);

        assertEquals("New Name", result.getDisplayName());
        assertEquals("USER", result.getRoles()); // unchanged
    }

    @Test
    void updateUser_wrongTenant_throws() {
        var user = UserEntity.builder().userId("u1").tenantId("t2").build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> svc.updateUser("t1", "u1", "Name", null, null, null));
    }

    @Test
    void updateUser_notFound_throws() {
        when(userRepo.findById("bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.updateUser("t1", "bad", "Name", null, null, null));
    }

    // ── resetPassword ────────────────────────────────────────────────────────

    @Test
    void resetPassword_encodesNewPassword() {
        var user = UserEntity.builder().userId("u1").tenantId("t1").passwordHash("old").build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("$newhash$");
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.resetPassword("t1", "u1", "newpass");

        assertEquals("$newhash$", result.getPasswordHash());
    }

    @Test
    void resetPassword_wrongTenant_throws() {
        var user = UserEntity.builder().userId("u1").tenantId("t2").build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> svc.resetPassword("t1", "u1", "newpass"));
    }

    // ── checkPassword ────────────────────────────────────────────────────────

    @Test
    void checkPassword_matches() {
        var user = UserEntity.builder().userId("u1").passwordHash("$hash$").build();
        when(passwordEncoder.matches("correct", "$hash$")).thenReturn(true);

        assertTrue(svc.checkPassword(user, "correct"));
    }

    @Test
    void checkPassword_doesNotMatch() {
        var user = UserEntity.builder().userId("u1").passwordHash("$hash$").build();
        when(passwordEncoder.matches("wrong", "$hash$")).thenReturn(false);

        assertFalse(svc.checkPassword(user, "wrong"));
    }

    // ── getUserById ──────────────────────────────────────────────────────────

    @Test
    void getUserById_tenantFilter() {
        var user = UserEntity.builder().userId("u1").tenantId("t1").build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertTrue(svc.getUserById("t1", "u1").isPresent());
        assertTrue(svc.getUserById("t2", "u1").isEmpty());
    }
}
