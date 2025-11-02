package com.marketplace.notification.service;

import com.marketplace.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

    private final NotificationWebSocketService webSocketService;

    @Override
    public void send(Notification notification, Long userId, String templateMessage) {
        // Envoie instantané via WebSocket
        webSocketService.sendToUser(userId, notification);
    }

    @Override
    public boolean supportsChannel(String channel) {
        return "IN_APP".equalsIgnoreCase(channel);
    }
}