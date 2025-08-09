package com.devmh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EventBrokerTest {

    @Test
    public void testPublishToKafkaAndWebSocket() throws Exception {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        SimpMessagingTemplate simpMessagingTemplate = mock(SimpMessagingTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        CaseEventPublisher publisher = new CaseEventPublisher(kafkaTemplate, simpMessagingTemplate, objectMapper);

        CaseEventMessage message = new CaseEventMessage("case123", "userA", Instant.now(), Instant.now().plusSeconds(300));

        publisher.publish(message, CaseEventType.CASE_LOCKED);

        verify(kafkaTemplate, times(1)).send(eq("case.locked"), anyString());
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/case/locked"), eq(message));
    }
}
