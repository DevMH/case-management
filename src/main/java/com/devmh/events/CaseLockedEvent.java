package com.devmh.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class CaseLockedEvent extends ApplicationEvent {
    private final String caseId;
    private final String userId;
    private final Instant lockTime;
    private final Instant expirationTime;

    public CaseLockedEvent(Object source, String caseId, String userId, Instant lockTime, Instant expirationTime) {
        super(source);
        this.caseId = caseId;
        this.userId = userId;
        this.lockTime = lockTime;
        this.expirationTime = expirationTime;
    }
}
