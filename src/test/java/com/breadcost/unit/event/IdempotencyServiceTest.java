package com.breadcost.unit.event;

import com.breadcost.eventstore.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private IdempotencyService svc;

    @BeforeEach
    void setUp() {
        svc = new IdempotencyService();
    }

    @Test
    void checkIdempotency_noRecord_returnsNull() {
        assertNull(svc.checkIdempotency("t1", "CreateOrder", "key-1"));
    }

    @Test
    void recordAndCheck_returnsResultRef() {
        svc.recordExecution("t1", "CreateOrder", "key-1", "order-42");

        assertEquals("order-42", svc.checkIdempotency("t1", "CreateOrder", "key-1"));
    }

    @Test
    void differentTenant_differentNamespace() {
        svc.recordExecution("t1", "CreateOrder", "key-1", "ref-A");
        svc.recordExecution("t2", "CreateOrder", "key-1", "ref-B");

        assertEquals("ref-A", svc.checkIdempotency("t1", "CreateOrder", "key-1"));
        assertEquals("ref-B", svc.checkIdempotency("t2", "CreateOrder", "key-1"));
    }

    @Test
    void differentCommand_differentNamespace() {
        svc.recordExecution("t1", "CreateOrder", "key-1", "ref-A");
        svc.recordExecution("t1", "CancelOrder", "key-1", "ref-B");

        assertEquals("ref-A", svc.checkIdempotency("t1", "CreateOrder", "key-1"));
        assertEquals("ref-B", svc.checkIdempotency("t1", "CancelOrder", "key-1"));
    }

    @Test
    void overwrite_returnsLatestRef() {
        svc.recordExecution("t1", "Cmd", "k", "first");
        svc.recordExecution("t1", "Cmd", "k", "second");

        assertEquals("second", svc.checkIdempotency("t1", "Cmd", "k"));
    }

    @Test
    void unrelatedKey_returnsNull() {
        svc.recordExecution("t1", "Cmd", "key-A", "ref");

        assertNull(svc.checkIdempotency("t1", "Cmd", "key-B"));
    }
}
