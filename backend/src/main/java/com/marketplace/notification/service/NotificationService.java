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
            case MESSAGE: {
                // Utilisé pour les notifications personnalisées des experts
                return getStringOrDefault(data, "message", "Vous avez un nouveau message.");
            }

            case GENERIC: {
                // Notifications génériques
                return getStringOrDefault(data, "message", "Vous avez une nouvelle notification.");
            }

            // ============ ENCHÈRES ============
            case AUCTION_START: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String startPrice = data.get("startPrice") != null
                        ? String.format("%.2f DH", data.get("startPrice"))
                        : "";

                return "🔨 Nouvelle enchère disponible !\n\n" +
                        "Produit : \"" + productName + "\"\n" +
                        (startPrice.isEmpty() ? "" : "Prix de départ : " + startPrice + "\n") +
                        "Cliquez pour participer à l'enchère !";
            }

            case NEW_BID: {
                String bidUser = getStringOrDefault(data, "bidUserName", "Un utilisateur");
                String bidAmount = data.get("bidAmount") != null
                        ? String.format("%.2f DH", data.get("bidAmount"))
                        : "";
                String productName = getStringOrDefault(data, "productName", "Produit");

                return "💰 Nouvelle enchère sur \"" + productName + "\"\n\n" +
                        bidUser + " a placé une offre de " + bidAmount;
            }

            case OUTBID: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String newBidAmount = data.get("newBidAmount") != null
                        ? String.format("%.2f DH", data.get("newBidAmount"))
                        : "";

                return "⚠️ Vous avez été surenchéri !\n\n" +
                        "Produit : \"" + productName + "\"\n" +
                        "Nouvelle offre : " + newBidAmount + "\n\n" +
                        "Placez une nouvelle enchère pour rester en tête !";
            }

            case AUCTION_WON: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String finalPrice = data.get("finalPrice") != null
                        ? String.format("%.2f DH", data.get("finalPrice"))
                        : "";

                return "🎉 Félicitations ! Vous avez remporté l'enchère !\n\n" +
                        "Produit : \"" + productName + "\"\n" +
                        "Prix final : " + finalPrice + "\n\n" +
                        "Procédez au paiement pour finaliser votre achat.";
            }

            case AUCTION_END: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                Boolean hasWinner = (Boolean) data.getOrDefault("hasWinner", false);

                if (hasWinner) {
                    return "🔨 L'enchère pour \"" + productName + "\" est terminée.\n" +
                            "Votre produit a été vendu avec succès !";
                } else {
                    return "🔨 L'enchère pour \"" + productName + "\" est terminée.\n" +
                            "Aucune offre n'a été faite. Vous pouvez relancer une nouvelle enchère.";
                }
            }

            // ============ TRANSACTIONS ============
            case PRODUCT_DELIVERED: {
                String productName = getStringOrDefault(data, "productName", "Produit");

                return "📦 Livraison confirmée\n\n" +
                        "Le produit \"" + productName + "\" a été livré avec succès.\n" +
                        "N'oubliez pas de confirmer la réception.";
            }

            case PRODUCT_SOLD: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String amount = data.get("amount") != null
                        ? String.format("%.2f DH", data.get("amount"))
                        : "";

                return "💰 Vente réussie !\n\n" +
                        "Votre produit \"" + productName + "\" a été vendu" +
                        (amount.isEmpty() ? "." : " pour " + amount + ".") + "\n" +
                        "Les fonds seront crédités sur votre portefeuille.";
            }

            case PAYMENT_RECEIVED: {
                String productName = getStringOrDefault(data, "productName", "");
                Double amount = (Double) data.get("amount");
                String amountStr = amount != null ? String.format("%.2f DH", amount) : "";

                StringBuilder msg = new StringBuilder("💵 Paiement reçu : ").append(amountStr);
                if (!productName.isEmpty()) {
                    msg.append("\n\nProduit : \"").append(productName).append("\"");
                }
                return msg.toString();
            }

            case PAYMENT_SENT: {
                String productName = getStringOrDefault(data, "productName", "");
                Double amount = (Double) data.get("amount");
                String amountStr = amount != null ? String.format("%.2f DH", amount) : "";

                StringBuilder msg = new StringBuilder("💳 Paiement effectué : ").append(amountStr);
                if (!productName.isEmpty()) {
                    msg.append("\n\nProduit : \"").append(productName).append("\"");
                }
                return msg.toString();
            }

            // ============ COMPTE ============
            case ACCOUNT_UPDATED: {
                String details = getStringOrDefault(data, "details", "");

                return "✅ Votre profil a été mis à jour avec succès." +
                        (details.isEmpty() ? "" : "\n\n" + details);
            }

            case ADMIN_ALERT: {
                String adminMessage = getStringOrDefault(data, "adminMessage", "Message de l'administrateur.");

                return "⚠️ Message administrateur\n\n" + adminMessage;
            }

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