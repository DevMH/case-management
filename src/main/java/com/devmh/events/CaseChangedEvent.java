package com.devmh.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;
import java.util.List;

@Getter
public class CaseChangedEvent extends ApplicationEvent {
    private final UUID caseId;
    private final List<String> changes;
    private final String user;

    public CaseChangedEvent(Object source, UUID caseId, List<String> changes, String user) {
        super(source);
        this.caseId = caseId;
        this.changes = changes;
        this.user = user;
    }

}
