package com.devmh.events;

import com.devmh.events.kafka.KafkaEventPublisher;
import com.devmh.events.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventBus {

    private final KafkaEventPublisher kafkaPublisher;
    private final WebSocketNotifier notifier;

    @Async
    @EventListener
    public void onCaseChanged(CaseChangedEvent event) {
        kafkaPublisher.send(event);
        notifier.notify(event);
    }

    @Async
    @EventListener
    public void onCaseLocked(CaseLockedEvent event) {
        notifier.notify(event);
    }
}

