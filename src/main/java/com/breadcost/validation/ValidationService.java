package com.breadcost.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Validation service
 * Implements governance rules from MASTERDATA_VALIDATION_RULES, etc.
 */
@Service
@Slf4j
public class ValidationService {

    /**
     * Validate that item is not a sentinel
     * Per SENTINEL_ITEM_RULES: sentinels excluded from costing/recipes/purchasing
     */
    public void validateNotSentinel(String itemId) {
        if (itemId.startsWith("__") && itemId.endsWith("__")) {
            throw new IllegalArgumentException("Sentinel items cannot be used in transactions: " + itemId);
        }
    }

    /**
     * Validate lot reference includes siteId
     * Per LOT_REFERENCE_LAW
     */
    public void validateLotReference(String siteId, String lotId) {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("siteId required for lot reference per LOT_REFERENCE_LAW");
        }
        if (lotId == null || lotId.isBlank()) {
            throw new IllegalArgumentException("lotId required");
        }
    }

    /**
     * Validate idempotency key format
     */
    public void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey required for all commands");
        }
        if (idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("idempotencyKey too long (max 255 chars)");
        }
    }
}
