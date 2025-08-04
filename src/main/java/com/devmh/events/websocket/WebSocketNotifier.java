package com.devmh.events.websocket;

import com.devmh.events.CaseChangedEvent;
import com.devmh.events.CaseLockedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketNotifier {

    @Autowired
    private SimpMessagingTemplate template;

    public void notify(CaseChangedEvent event) {
        template.convertAndSend("/topic/cases/" + event.getCaseId(), event);
    }

    public void notify(CaseLockedEvent event) {
        template.convertAndSend("/topic/locks/" + event.getCaseId(), event);
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
