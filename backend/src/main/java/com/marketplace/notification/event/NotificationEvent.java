package com.marketplace.notification.event;

import com.marketplace.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

/**
 * Evénement transportant le type, la liste des destinataires et les données à rendre.
 * Exemple d'utilisation :
 * applicationEventPublisher.publishEvent(new NotificationEvent(NotificationType.GENERIC, Set.of(userId), Map.of("message","...")));
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private NotificationType type;
    private Set<Long> recipientIds;
    private Map<String, Object> data;
}