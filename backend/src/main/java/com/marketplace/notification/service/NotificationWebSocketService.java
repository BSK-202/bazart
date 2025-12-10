package com.marketplace.notification.service;

import com.marketplace.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long userId, Notification notification) {
        // Utiliser "/topic" comme configuré dans WebSocketConfig
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
    }
}
