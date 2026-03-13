package com.breadcost.unit.security;

import com.breadcost.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "breadcost-super-secret-key-for-jwt-signing-at-least-32-bytes");
        ReflectionTestUtils.setField(jwtUtil, "expiryMs", 86400000L);
    }

    @Test
    void generate_createsValidToken() {
        String token = jwtUtil.generate("admin", List.of("ADMIN", "MANAGER"), "tenant1");

        assertNotNull(token);
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void getUsername_returnsCorrectSubject() {
        String token = jwtUtil.generate("john", List.of("CASHIER"), "t1");

        assertEquals("john", jwtUtil.getUsername(token));
    }

    @Test
    void getRoles_returnsAllRoles() {
        String token = jwtUtil.generate("jane", List.of("ADMIN", "FINANCE"), "t1");

        List<String> roles = jwtUtil.getRoles(token);

        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("FINANCE"));
    }

    @Test
    void getTenantId_returnsCorrectTenant() {
        String token = jwtUtil.generate("user", List.of("CASHIER"), "tenant42");

        assertEquals("tenant42", jwtUtil.getTenantId(token));
    }

    @Test
    void parse_returnsValidClaims() {
        String token = jwtUtil.generate("bob", List.of("WAREHOUSE"), "t5");

        Claims claims = jwtUtil.parse(token);

        assertEquals("bob", claims.getSubject());
        assertEquals("t5", claims.get("tenantId"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        String token = jwtUtil.generate("user", List.of("ADMIN"), "t1");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void isValid_returnsFalseForNullToken() {
        assertFalse(jwtUtil.isValid(null));
    }

    @Test
    void isValid_returnsFalseForEmptyToken() {
        assertFalse(jwtUtil.isValid(""));
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiryMs", -1000L);
        String token = jwtUtil.generate("user", List.of("ADMIN"), "t1");

        assertFalse(jwtUtil.isValid(token));
    }

    @Test
    void isValid_returnsFalseForWrongKey() {
        String token = jwtUtil.generate("user", List.of("ADMIN"), "t1");

        JwtUtil otherUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherUtil, "secret",
                "different-secret-key-that-is-at-least-32-bytes-long-ok");
        ReflectionTestUtils.setField(otherUtil, "expiryMs", 86400000L);

        assertFalse(otherUtil.isValid(token));
    }
}
