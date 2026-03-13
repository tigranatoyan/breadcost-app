package com.breadcost.unit.validation;

import com.breadcost.validation.ValidationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private final ValidationService svc = new ValidationService();

    // ── validateNotSentinel ──────────────────────────────────────────────────

    @Test
    void validateNotSentinel_normalItem_noException() {
        assertDoesNotThrow(() -> svc.validateNotSentinel("flour-001"));
    }

    @Test
    void validateNotSentinel_sentinelItem_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> svc.validateNotSentinel("__OVERHEAD__"));
        assertTrue(ex.getMessage().contains("Sentinel items"));
    }

    @Test
    void validateNotSentinel_singleUnderscorePrefix_allowed() {
        assertDoesNotThrow(() -> svc.validateNotSentinel("_partial"));
    }

    // ── validateLotReference ─────────────────────────────────────────────────

    @Test
    void validateLotReference_valid_noException() {
        assertDoesNotThrow(() -> svc.validateLotReference("site-1", "LOT-001"));
    }

    @Test
    void validateLotReference_nullSiteId_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> svc.validateLotReference(null, "LOT-001"));
        assertTrue(ex.getMessage().contains("siteId required"));
    }

    @Test
    void validateLotReference_blankSiteId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validateLotReference("  ", "LOT-001"));
    }

    @Test
    void validateLotReference_nullLotId_throws() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> svc.validateLotReference("site-1", null));
        assertTrue(ex.getMessage().contains("lotId required"));
    }

    // ── validateIdempotencyKey ───────────────────────────────────────────────

    @Test
    void validateIdempotencyKey_valid_noException() {
        assertDoesNotThrow(() -> svc.validateIdempotencyKey("key-123"));
    }

    @Test
    void validateIdempotencyKey_null_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validateIdempotencyKey(null));
    }

    @Test
    void validateIdempotencyKey_blank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validateIdempotencyKey("   "));
    }

    @Test
    void validateIdempotencyKey_tooLong_throws() {
        String longKey = "K".repeat(256);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> svc.validateIdempotencyKey(longKey));
        assertTrue(ex.getMessage().contains("too long"));
    }

    @Test
    void validateIdempotencyKey_maxLength_allowed() {
        String key = "K".repeat(255);
        assertDoesNotThrow(() -> svc.validateIdempotencyKey(key));
    }
}
