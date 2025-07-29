package com.devmh.events.websocket;

import com.devmh.events.CaseChangedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Component
public class WebSocketNotifier {

    private final SimpMessagingTemplate template;

    public WebSocketNotifier(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void notify(CaseChangedEvent event) {
        template.convertAndSend("/topic/cases/" + event.getCaseId(), event);
    }
}

/*
const socket = new SockJS('/ws-case-updates');
const stomp = Stomp.over(socket);

stomp.connect({}, () => {
  stomp.subscribe('/topic/cases/1234-uuid', message => {
    const update = JSON.parse(message.body);
    console.log("Received update:", update);
  });
});
 */
