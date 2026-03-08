package com.breadcost.multitenancy;

/**
 * Thread-local holder for the current tenant ID.
 * Set by {@link TenantFilter} from the JWT on every request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
