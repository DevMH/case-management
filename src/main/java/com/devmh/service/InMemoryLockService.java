package com.devmh.service;

import com.devmh.events.CaseLockedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class InMemoryLockService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final class LockInfo {
        final String userId;
        Instant expiration;
        final ReentrantLock lock = new ReentrantLock(true);

        LockInfo(String userId, Instant expiration) {
            this.userId = userId;
            this.expiration = expiration;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiration);
        }

        void extend(Instant newExpiration) {
            this.expiration = newExpiration;
        }
    }

    private final Map<UUID, LockInfo> locks = new ConcurrentHashMap<>();

    @Value("${lock.expiration.seconds:30}")
    public long expirationSeconds;

    public boolean tryLock(UUID caseId, String userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        LockInfo lockInfo = locks.compute(caseId, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LockInfo(userId, expiry);
            }
            if (existing.userId.equals(userId)) {
                existing.extend(expiry);
                return existing;
            }
            return existing;
        });

        if (!lockInfo.userId.equals(userId)) {
            log.info("Case {} is locked by another user: {}", caseId, lockInfo.userId);
            return false;
        }

        boolean acquired = lockInfo.lock.tryLock();
        if (!acquired && lockInfo.lock.isHeldByCurrentThread()) {
            // Reentrant acquisition
            lockInfo.lock.lock();
            acquired = true;
        }

        if (acquired) {
            eventPublisher.publishEvent(new CaseLockedEvent(caseId, userId, now, expiry));
            log.info("Lock acquired for case {} by user {}", caseId, userId);
        }

        return acquired;
    }

    public void unlock(UUID caseId, String userId) {
        LockInfo info = locks.get(caseId);
        if (info != null && info.userId.equals(userId) && info.lock.isHeldByCurrentThread()) {
            info.lock.unlock();
            log.info("Lock released for case {} by user {}", caseId, userId);
            if (!info.lock.isLocked()) {
                locks.remove(caseId);
            }
        } else {
            log.warn("Unlock failed for case {} by user {} (not owner or not locked)", caseId, userId);
        }
    }

    public boolean isLocked(UUID caseId) {
        LockInfo info = locks.get(caseId);
        return info != null && !info.isExpired();
    }

    public String lockedBy(UUID caseId) {
        LockInfo info = locks.get(caseId);
        return (info != null && !info.isExpired()) ? info.userId : null;
    }
}

