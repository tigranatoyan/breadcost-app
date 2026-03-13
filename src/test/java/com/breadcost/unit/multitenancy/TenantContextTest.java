package com.breadcost.unit.multitenancy;

import com.breadcost.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_returnsTenantId() {
        TenantContext.setTenantId("tenant1");

        assertEquals("tenant1", TenantContext.getTenantId());
    }

    @Test
    void clear_removesValue() {
        TenantContext.setTenantId("tenant1");
        TenantContext.clear();

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void get_returnsNullWhenNotSet() {
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void set_overwritesPrevious() {
        TenantContext.setTenantId("A");
        TenantContext.setTenantId("B");

        assertEquals("B", TenantContext.getTenantId());
    }

    @Test
    void threadIsolation() throws Exception {
        TenantContext.setTenantId("main-tenant");

        Thread child = new Thread(() -> {
            assertNull(TenantContext.getTenantId());
            TenantContext.setTenantId("child-tenant");
            assertEquals("child-tenant", TenantContext.getTenantId());
        });
        child.start();
        child.join();

        assertEquals("main-tenant", TenantContext.getTenantId());
    }
}
