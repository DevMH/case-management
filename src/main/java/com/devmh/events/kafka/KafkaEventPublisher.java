package com.devmh.events.kafka;

import com.devmh.events.CaseChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, CaseChangedEvent> kafkaTemplate;

    public void send(CaseChangedEvent event) {
        kafkaTemplate.send("case-changes", event.getCaseId().toString(), event);
    }
}
