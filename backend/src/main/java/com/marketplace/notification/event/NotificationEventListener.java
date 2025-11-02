package com.marketplace.notification.event;

import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationSender;
import com.marketplace.notification.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        notificationService.processEvent(event.getType(), event.getRecipientIds(), event.getData());
    }
}
