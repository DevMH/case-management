package com.devmh;

import com.devmh.service.InMemoryLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLockServiceTest {

    private InMemoryLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new InMemoryLockService();
    }

    @Test
    void testLockAndUnlock() {
        UUID caseId = UUID.randomUUID();
        String user = "alice";

        assertTrue(lockService.tryLock(caseId, user));
        assertTrue(lockService.isLocked(caseId));
        assertEquals("alice", lockService.lockedBy(caseId));

        lockService.unlock(caseId, user);
        assertFalse(lockService.isLocked(caseId));
        assertNull(lockService.lockedBy(caseId));
    }

    @Test
    void testReentrantLockingBySameUser() {
        UUID caseId = UUID.randomUUID();
        String user = "bob";

        assertTrue(lockService.tryLock(caseId, user));
        assertTrue(lockService.tryLock(caseId, user));

        lockService.unlock(caseId, user);
        assertTrue(lockService.isLocked(caseId)); // still held once
        lockService.unlock(caseId, user);
        assertFalse(lockService.isLocked(caseId)); // now fully unlocked
    }

    @Test
    void testDifferentUserFailsToLock() {
        UUID caseId = UUID.randomUUID();
        assertTrue(lockService.tryLock(caseId, "alice"));
        assertFalse(lockService.tryLock(caseId, "bob"));
    }

    @Test
    void testUnlockByWrongUserDoesNothing() {
        UUID caseId = UUID.randomUUID();
        lockService.tryLock(caseId, "alice");

        lockService.unlock(caseId, "bob");
        assertTrue(lockService.isLocked(caseId));
    }

    @Test
    void testLockExpiration() throws InterruptedException {
        UUID caseId = UUID.randomUUID();
        String user = "alice";

        lockService.expirationSeconds = 1;
        assertTrue(lockService.tryLock(caseId, user));

        Thread.sleep(2000); // wait until expired
        assertFalse(lockService.isLocked(caseId));
        assertNull(lockService.lockedBy(caseId));

        assertTrue(lockService.tryLock(caseId, "bob")); // should succeed now
        assertEquals("bob", lockService.lockedBy(caseId));
    }
}

