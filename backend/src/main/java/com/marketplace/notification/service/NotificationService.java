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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    // You may still use templateService for advanced templating if needed
    private final List<NotificationSender> senders;
    private final UserNotificationPreferenceService preferenceService;

    public NotificationService(
            NotificationRepository notificationRepository,
            List<NotificationSender> senders,
            UserNotificationPreferenceService preferenceService
    ) {
        this.notificationRepository = notificationRepository;
        this.senders = senders;
        this.preferenceService = preferenceService;

        // Debug injectés
        System.out.println("NotificationSenders injectés: " + senders);
        for (NotificationSender s : senders) {
            System.out.println("→ Sender: " + s.getClass().getName());
        }
    }

    /**
     * Main method to process events and send notifications.
     * Customizes content based on type and provided data.
     */
    public void processEvent(NotificationType type, Set<Long> recipientIds, Map<String, Object> data)
    {
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
            System.out.println("→ Vérif notif email, userId=" + userId + ", emailEnabled? " + pref.isEmailEnabled());
            // Email channel
            if (pref.isEmailEnabled()) {
                System.out.println("→ Vérif notif email, userId=" + userId + ", emailEnabled? " + pref.isEmailEnabled());
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
            // produit accepté MAIS expertise obligatoire
            case PRODUCT_EXPERTISE_REQUIRED: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                String method = (String) data.getOrDefault("expertiseMethod", "en ligne ou présentielle");
                return "Votre produit \"" + productName + "\" a été accepté.\n" +
                        "Vous avez choisi une expertise " + method + ". " +
                        "Merci de planifier une date d'expertise dans votre espace vendeur.";
            }
            // quand tu planifieras la date (plus tard)
            case PRODUCT_EXPERTISE_PLANNED: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                String dateTime = (String) data.getOrDefault("dateTime", "une date à venir");
                String method = (String) data.getOrDefault("expertiseMethod", "en ligne");
                return "L'expertise " + method + " pour votre produit \"" + productName +
                        "\" a été planifiée le " + dateTime + ".";
            }
            case AUCTION_START:
                String auctionProduct = (String) data.getOrDefault("productName", "Produit");
                return "Une nouvelle enchère commence pour \"" + auctionProduct + "\".";
            case AUCTION_END: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                String winnerName = (String) data.getOrDefault("winnerName", "un acheteur");
                Object winningAmount = data.getOrDefault("winningAmount", null);
                String amountStr = winningAmount != null ? String.format("%.2f DH", winningAmount) : "";

                return "⏰ L'enchère est terminée !\n" +
                        "🏆 Produit: \"" + productName + "\"\n" +
                        "Gagnant: " + winnerName +
                        (amountStr.isEmpty() ? "" : "\nMontant gagnant: " + amountStr) + "\n" +
                        "Merci pour votre participation !";
            }
            case NEW_BID:
                String bidUser = (String) data.getOrDefault("bidUserName", "Un utilisateur");
                String bidAmount = String.valueOf(data.getOrDefault("bidAmount", ""));
                String bidProduct = (String) data.getOrDefault("productName", "Produit");
                return bidUser + " a placé une nouvelle enchère de " + bidAmount + " DH sur \"" + bidProduct + "\".";
            case OUTBID:
                String outbidProduct = (String) data.getOrDefault("productName", "Produit");
                return "Vous avez été surenchéri sur \"" + outbidProduct + "\".";
            case AUCTION_WON: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                Object winningAmount = data.getOrDefault("winningAmount", null);
                String amountStr = winningAmount != null ? String.format("%.2f DH", winningAmount) : "";

                return "🏆 Félicitations ! Vous avez gagné l'enchère pour \"" + productName + "\"" +
                        (amountStr.isEmpty() ? " !" : " pour " + amountStr + " !") +
                        "\nContactez le vendeur pour finaliser la transaction.";
            }
            case PRODUCT_DELIVERED:
                String deliveredProduct = (String) data.getOrDefault("productName", "Produit");
                return "Votre produit \"" + deliveredProduct + "\" a été livré.";
            case PRODUCT_SOLD: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                Object winningAmount = data.getOrDefault("winningAmount", null);
                String amountStr = winningAmount != null ? String.format("%.2f DH", winningAmount) : "";
                String winnerName = (String) data.getOrDefault("winnerName", "un acheteur");

                return "💰 l' enchère de votre produit \"" + productName + "\" a été terminéé" +
                        (amountStr.isEmpty() ? " !" : " pour " + amountStr + " !") +
                        "\nLe gagnant "+ winnerName +" va vous contacter pour la livraison.";
            }
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
            case TRANSACTION_ACCEPTED: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                String buyerName = (String) data.getOrDefault("buyerName", "un acheteur");
                Object amount = data.getOrDefault("amount", null);
                String amountStr = amount != null ? String.format("%.2f DH", amount) : "";

                return "✅ Transaction acceptée !\n" +
                        "Votre produit \"" + productName + "\" a été accepté par le gagnant " +
                        buyerName + ".\n" +
                        (amountStr.isEmpty() ? "" : "Vous avez été remboursé de " + amountStr + ".\n") +
                        "La transaction est maintenant complète.";
            }
            case TRANSACTION_ANNULEE: {
                System.out.println("🎯 ENTRÉE dans case TRANSACTION_ANNULEE");
                String productName = (String) data.getOrDefault("productName", "Produit");
                String buyerName = (String) data.getOrDefault("buyerName", "un acheteur");
                Object amount = data.getOrDefault("amount", null);
                String amountStr = amount != null ? String.format("%.2f DH", amount) : "";

                return "❌ Transaction annulée !\n" +
                        "La transaction pour votre produit \"" + productName + "\" avec " +
                        buyerName + " a été annulée.\n" +
                        (amountStr.isEmpty() ? "" : "Le montant de " + amountStr + " a été remboursé à ") +buyerName+
                        "La transaction est maintenant terminée.";
            }
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