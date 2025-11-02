package com.marketplace.notification.service;

import com.marketplace.notification.entity.Notification;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.entity.UserNotificationPreference;
import com.marketplace.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    // You may still use templateService for advanced templating if needed
    private final List<NotificationSender> senders;
    private final UserNotificationPreferenceService preferenceService;

    /**
     * Main method to process events and send notifications.
     * Customizes content based on type and provided data.
     */
    public void processEvent(NotificationType type, Set<Long> recipientIds, Map<String, Object> data) {
        String rendered = buildMessageForType(type, data);

        for (Long userId : recipientIds) {
            Notification notification = Notification.builder()
                .recipientId(userId)
                .type(type)
                .message(rendered)
                .read(false)
                .createdAt(java.time.LocalDateTime.now())
                .build();
            notificationRepository.save(notification);

            // Respect user notification preferences
            UserNotificationPreference pref = preferenceService.getOrDefault(userId);

            // In-App channel
            if (pref.isInAppEnabled()) {
                for (NotificationSender sender : senders) {
                    if (sender.supportsChannel("IN_APP")) {
                        try {
                            sender.send(notification, userId, rendered);
                        } catch (Exception ex) {
                            System.err.println("Erreur envoi IN_APP pour user " + userId + ": " + ex.getMessage());
                        }
                    }
                }
            }

            // Email channel
            if (pref.isEmailEnabled()) {
                for (NotificationSender sender : senders) {
                    if (sender.supportsChannel("EMAIL")) {
                        try {
                            sender.send(notification, userId, rendered);
                        } catch (Exception ex) {
                            System.err.println("Erreur envoi EMAIL pour user " + userId + ": " + ex.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Customizes notification content/message based on type and dynamic data.
     */
    private String buildMessageForType(NotificationType type, Map<String, Object> data) {
        switch (type) {
	        case PRODUCT_ACCEPTED: {
	            String productNameAccepted = (String) data.getOrDefault("productName", "Produit");
	            String adminMessage = (String) data.getOrDefault("adminMessage", "");
	            String msg = "Votre produit \"" + productNameAccepted + "\" a été accepté par l'administrateur.";
	            if (adminMessage != null && !adminMessage.trim().isEmpty()) {
	                msg += "\nNote de l'administrateur : " + adminMessage;
	            }
	            return msg;
	        }
	        case PRODUCT_REFUSED: {
	            String productNameRefused = (String) data.getOrDefault("productName", "Produit");
	            String adminMessage = (String) data.getOrDefault("adminMessage", "");
	            String msg = "Votre produit \"" + productNameRefused + "\" a été refusé.";
	            if (adminMessage != null && !adminMessage.trim().isEmpty()) {
	                msg += "\nNote de l'administrateur : " + adminMessage;
	            }
	            return msg;
	        }
            case AUCTION_START:
                String auctionProduct = (String) data.getOrDefault("productName", "Produit");
                return "Une nouvelle enchère commence pour \"" + auctionProduct + "\".";
            case NEW_BID:
                String bidUser = (String) data.getOrDefault("bidUserName", "Un utilisateur");
                String bidAmount = String.valueOf(data.getOrDefault("bidAmount", ""));
                String bidProduct = (String) data.getOrDefault("productName", "Produit");
                return bidUser + " a placé une nouvelle enchère de " + bidAmount + " DH sur \"" + bidProduct + "\".";
            case OUTBID:
                String outbidProduct = (String) data.getOrDefault("productName", "Produit");
                return "Vous avez été surenchéri sur \"" + outbidProduct + "\".";
            case AUCTION_WON:
                String auctionWonProduct = (String) data.getOrDefault("productName", "Produit");
                return "Félicitations ! Vous avez remporté l'enchère pour \"" + auctionWonProduct + "\".";
            case PRODUCT_DELIVERED:
                String deliveredProduct = (String) data.getOrDefault("productName", "Produit");
                return "Votre produit \"" + deliveredProduct + "\" a été livré.";
            case PRODUCT_SOLD:
                String soldProduct = (String) data.getOrDefault("productName", "Produit");
                return "Votre produit \"" + soldProduct + "\" a été vendu.";
            case ADMIN_ALERT:
                String adminMessage = (String) data.getOrDefault("adminMessage", "Message de l'administrateur.");
                return "Message de l'administrateur : " + adminMessage;
            case ACCOUNT_UPDATED:
                return "Votre profil ou paramètres ont été mis à jour avec succès.";
            case PAYMENT_RECEIVED:
                String paymentProductReceived = (String) data.getOrDefault("productName", "Produit");
                String paymentAmountReceived = String.valueOf(data.getOrDefault("amount", ""));
                return "Paiement de " + paymentAmountReceived + " DH reçu pour \"" + paymentProductReceived + "\".";
            case PAYMENT_SENT:
                String paymentProductSent = (String) data.getOrDefault("productName", "Produit");
                String paymentAmountSent = String.valueOf(data.getOrDefault("amount", ""));
                return "Vous avez envoyé un paiement de " + paymentAmountSent + " DH pour \"" + paymentProductSent + "\".";
            case MESSAGE:
                String customMessage = (String) data.getOrDefault("message", "Vous avez un nouveau message.");
                return customMessage;
            case GENERIC:
            default:
                return "Vous avez une nouvelle notification.";
        }
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}