package com.breadcost.unit.service;

import com.breadcost.customers.*;
import com.breadcost.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepo;
    @Mock private PasswordResetTokenRepository resetTokenRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private CustomerService svc;

    // ── registerCustomer ─────────────────────────────────────────────────────

    @Test
    void register_savesWithNormalizedEmail() {
        when(customerRepo.findByTenantIdAndEmail("t1", "john@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(customerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerEntity result = svc.registerCustomer("t1", " John ", " John@Test.COM ", "pass123", "+1234", null);

        assertEquals("john@test.com", result.getEmail());
        assertEquals("John", result.getName());
        verify(customerRepo).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(customerRepo.findByTenantIdAndEmail("t1", "dup@test.com"))
                .thenReturn(Optional.of(new CustomerEntity()));

        assertThrows(IllegalStateException.class,
                () -> svc.registerCustomer("t1", "Dup", "dup@test.com", "pass", null, null));
    }

    @Test
    void register_nullPassword_savesNullHash() {
        when(customerRepo.findByTenantIdAndEmail(eq("t1"), any())).thenReturn(Optional.empty());
        when(customerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerEntity result = svc.registerCustomer("t1", "Jane", "jane@test.com", null, null, null);

        assertNull(result.getPasswordHash());
        verify(passwordEncoder, never()).encode(any());
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsToken() {
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("c1").tenantId("t1").email("a@b.com")
                .passwordHash("hashed").active(true).build();
        when(customerRepo.findByTenantIdAndEmail("t1", "a@b.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtUtil.generate("c1", List.of("Customer"), "t1")).thenReturn("jwt-token");

        assertEquals("jwt-token", svc.login("t1", "a@b.com", "pass"));
    }

    @Test
    void login_wrongPassword_throws() {
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("c1").passwordHash("hashed").active(true).build();
        when(customerRepo.findByTenantIdAndEmail("t1", "a@b.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> svc.login("t1", "a@b.com", "wrong"));
    }

    @Test
    void login_inactiveAccount_throws() {
        CustomerEntity customer = CustomerEntity.builder()
                .customerId("c1").active(false).build();
        when(customerRepo.findByTenantIdAndEmail("t1", "a@b.com")).thenReturn(Optional.of(customer));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> svc.login("t1", "a@b.com", "pass"));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void login_unknownEmail_throws() {
        when(customerRepo.findByTenantIdAndEmail("t1", "unknown@b.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> svc.login("t1", "unknown@b.com", "pass"));
    }

    // ── getProfile ───────────────────────────────────────────────────────────

    @Test
    void getProfile_found() {
        CustomerEntity c = CustomerEntity.builder().customerId("c1").tenantId("t1").build();
        when(customerRepo.findByTenantIdAndCustomerId("t1", "c1")).thenReturn(Optional.of(c));

        assertEquals("c1", svc.getProfile("t1", "c1").getCustomerId());
    }

    @Test
    void getProfile_notFound_throws() {
        when(customerRepo.findByTenantIdAndCustomerId("t1", "c99")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> svc.getProfile("t1", "c99"));
    }

    // ── forgotPassword ───────────────────────────────────────────────────────

    @Test
    void forgotPassword_generatesToken() {
        CustomerEntity c = CustomerEntity.builder().customerId("c1").build();
        when(customerRepo.findByTenantIdAndEmail("t1", "a@b.com")).thenReturn(Optional.of(c));
        when(resetTokenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token = svc.forgotPassword("t1", "a@b.com");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(resetTokenRepo).save(any());
    }

    @Test
    void forgotPassword_unknownEmail_throws() {
        when(customerRepo.findByTenantIdAndEmail("t1", "x@y.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> svc.forgotPassword("t1", "x@y.com"));
    }

    // ── resetPassword ────────────────────────────────────────────────────────

    @Test
    void resetPassword_validToken_updatesHash() {
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .id("t1").customerId("c1").token("tok").used(false)
                .expiresAtEpochMs(System.currentTimeMillis() + 60_000).build();
        CustomerEntity customer = CustomerEntity.builder().customerId("c1").build();

        when(resetTokenRepo.findByToken("tok")).thenReturn(Optional.of(token));
        when(customerRepo.findById("c1")).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("newpass")).thenReturn("newhash");

        svc.resetPassword("tok", "newpass");

        assertEquals("newhash", customer.getPasswordHash());
        assertTrue(token.isUsed());
        verify(customerRepo).save(customer);
    }

    @Test
    void resetPassword_expiredToken_throws() {
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .id("t1").customerId("c1").token("tok").used(false)
                .expiresAtEpochMs(System.currentTimeMillis() - 1000).build();
        when(resetTokenRepo.findByToken("tok")).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> svc.resetPassword("tok", "newpass"));
    }

    @Test
    void resetPassword_usedToken_throws() {
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .id("t1").customerId("c1").token("tok").used(true)
                .expiresAtEpochMs(System.currentTimeMillis() + 60_000).build();
        when(resetTokenRepo.findByToken("tok")).thenReturn(Optional.of(token));

        assertThrows(NoSuchElementException.class, () -> svc.resetPassword("tok", "newpass"));
    }
}
