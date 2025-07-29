package com.devmh.events;

import com.devmh.events.kafka.KafkaEventPublisher;
import com.devmh.events.websocket.WebSocketNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaseChangeEventListener {

    private final KafkaEventPublisher kafkaPublisher;
    private final WebSocketNotifier notifier;

    @EventListener
    public void onCaseChanged(CaseChangedEvent event) {
        kafkaPublisher.send(event);
        notifier.notify(event);
    }
}

