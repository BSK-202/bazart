package com.marketplace.notification.service;

import com.marketplace.notification.entity.Notification;

public interface NotificationSender {
    void send(Notification notification, Long userId, String templateMessage);
    boolean supportsChannel(String channel);
}
