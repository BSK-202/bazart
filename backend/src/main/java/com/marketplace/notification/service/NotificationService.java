package com.marketplace.notification.service;

import com.marketplace.notification.entity.Notification;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.entity.UserNotificationPreference;
import com.marketplace.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final List<NotificationSender> senders;
    private final UserNotificationPreferenceService preferenceService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    public NotificationService(
            NotificationRepository notificationRepository,
            List<NotificationSender> senders,
            UserNotificationPreferenceService preferenceService
    ) {
        this.notificationRepository = notificationRepository;
        this.senders = senders;
        this.preferenceService = preferenceService;

        System.out.println("NotificationSenders injectés: " + senders.size());
        for (NotificationSender s : senders) {
            System.out.println("→ Sender: " + s.getClass().getName());
        }
    }

    /**
     * Méthode principale pour traiter les événements et envoyer les notifications
     */
    public void processEvent(NotificationType type, Set<Long> recipientIds, Map<String, Object> data) {
        String rendered = buildMessageForType(type, data);

        for (Long userId : recipientIds) {
            Notification notification = Notification.builder()
                    .recipientId(userId)
                    .type(type)
                    .message(rendered)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            // Récupérer les préférences de l'utilisateur
            UserNotificationPreference pref = preferenceService.getOrDefault(userId);

            // Canal In-App
            if (pref.isInAppEnabled()) {
                sendToChannel("IN_APP", notification, userId, rendered);
            }

            // Canal Email
            if (pref.isEmailEnabled()) {
                sendToChannel("EMAIL", notification, userId, rendered);
            }
        }
    }

    /**
     * Envoie une notification via un canal spécifique
     */
    private void sendToChannel(String channel, Notification notification, Long userId, String message) {
        for (NotificationSender sender : senders) {
            if (sender.supportsChannel(channel)) {
                try {
                    sender.send(notification, userId, message);
                    System.out.println("✓ Notification " + channel + " envoyée à l'utilisateur " + userId);
                } catch (Exception ex) {
                    System.err.println("✗ Erreur envoi " + channel + " pour user " + userId + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Personnalise le contenu de la notification selon le type et les données
     */
    private String buildMessageForType(NotificationType type, Map<String, Object> data) {
        switch (type) {

            // ============ GESTION PRODUITS ============
            case PRODUCT_ACCEPTED: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String adminMessage = getStringOrDefault(data, "adminMessage", "");

                StringBuilder msg = new StringBuilder();
                msg.append("✅ Félicitations ! Votre produit \"").append(productName)
                        .append("\" a été accepté par l'administrateur.");

                if (!adminMessage.trim().isEmpty()) {
                    msg.append("\n\n📝 Note de l'administrateur :\n").append(adminMessage);
                }

                return msg.toString();
            }

            case PRODUCT_REFUSED: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String adminMessage = getStringOrDefault(data, "adminMessage", "");

                StringBuilder msg = new StringBuilder();
                msg.append("❌ Votre produit \"").append(productName)
                        .append("\" a été refusé par l'administrateur.");

                if (!adminMessage.trim().isEmpty()) {
                    msg.append("\n\n📝 Raison du refus :\n").append(adminMessage);
                }

                msg.append("\n\nVous pouvez modifier votre produit et le resoumettre.");

                return msg.toString();
            }

            // ============ EXPERTISE - NOTIFICATIONS VENDEUR ============
            case PRODUCT_EXPERTISE_REQUIRED: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String method = getStringOrDefault(data, "expertiseMethod", "ONLINE");

                String methodText = method.equals("ONLINE")
                        ? "en ligne (à distance)"
                        : "sur place (présentielle)";

                return "✅ Votre produit \"" + productName + "\" a été accepté !\n\n" +
                        "📋 Une expertise " + methodText + " est requise.\n" +
                        "Un expert sera assigné automatiquement et vous en serez notifié.";
            }

            case PRODUCT_EXPERTISE_PLANNED: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String dateTime = getStringOrDefault(data, "dateTime", null);
                String method = getStringOrDefault(data, "expertiseMethod", "en ligne");
                Double amount = (Double) data.get("amount");

                StringBuilder msg = new StringBuilder();
                msg.append("📅 Expertise planifiée pour \"").append(productName).append("\"\n\n");

                if (method.equals("en ligne") || method.equals("ONLINE")) {
                    msg.append("Type : Expertise en ligne (à distance)\n");
                    msg.append("L'expert procédera à l'analyse de votre produit en ligne.\n");
                } else {
                    msg.append("Type : Expertise présentielle (sur place)\n");
                    if (dateTime != null) {
                        msg.append("📆 Date du rendez-vous : ").append(formatDateTime(dateTime)).append("\n");
                        msg.append("📍 Lieu : Magasin Bazart\n");
                    }
                }

                if (amount != null) {
                    msg.append("\n💰 Montant : ").append(String.format("%.2f DH", amount));
                    msg.append("\n(Débité de votre portefeuille)");
                }

                msg.append("\n\nVous recevrez le rapport d'expertise dès qu'il sera disponible.");

                return msg.toString();
            }

            // ============ EXPERTISE - NOTIFICATIONS EXPERT ============

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
                return "🔔 Vous avez une nouvelle notification.";
        }
    }

    /**
     * Méthodes utilitaires
     */
    private String getStringOrDefault(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue != null ? defaultValue : "";
        }
        return value.toString();
    }

    private String formatDateTime(String dateTimeStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
            return dateTime.format(DATE_FORMATTER);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    /**
     * Récupère les notifications d'un utilisateur
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Marque une notification comme lue
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
    }
}