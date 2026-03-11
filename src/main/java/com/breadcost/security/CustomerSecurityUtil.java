package com.breadcost.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * Utility for customer own-data enforcement (BC-2603).
 * Ensures customers can only access their own resources.
 * Staff with Admin or Manager roles bypass the check.
 */
public final class CustomerSecurityUtil {

    private CustomerSecurityUtil() {}

    /**
     * Verify the authenticated user owns the given customerId, or is staff (Admin/Manager).
     * Throws 403 if a Customer tries to access another customer's data.
     */
    public static void assertOwner(String customerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String role = a.getAuthority();
                    return role.equals("ROLE_Admin") || role.equals("ROLE_Manager");
                });
        if (isStaff) return;

        String principal = auth.getName();
        if (!principal.equals(customerId)) {
            throw new AccessDeniedException("Customer can only access own data");
        }
    }

    /** Extract tenantId stored in auth details by JwtAuthFilter. */
    @SuppressWarnings("unchecked")
    public static String getTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Map) {
            return ((Map<String, String>) auth.getDetails()).get("tenantId");
        }
        return null;
    }
}
