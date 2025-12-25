package com.marketplace.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.notification.entity.Notification;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.entity.UserNotificationPreference;
import com.marketplace.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
            Map<String, Object> wsData = new HashMap<>(data);
            wsData.put("type", type.toString());
            wsData.put("message", rendered);
             wsData.put("productId", data.get("productId")); // S'assurer que productId est inclus

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
                msg.append("Félicitations ! Votre produit \"").append(productName)
                        .append("\" a été accepté par l'administrateur.");

                if (!adminMessage.trim().isEmpty()) {
                    msg.append("\n\nNote de l'administrateur :\n").append(adminMessage);
                }

                return msg.toString();
            }

            case PRODUCT_REFUSED: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String adminMessage = getStringOrDefault(data, "adminMessage", "");

                StringBuilder msg = new StringBuilder();
                msg.append("Votre produit \"").append(productName)
                        .append("\" a été refusé par l'administrateur.");

                if (!adminMessage.trim().isEmpty()) {
                    msg.append("\n\nRaison du refus :\n").append(adminMessage);
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

                return "Votre produit \"" + productName + "\" a été accepté !\n\n" +
                        "Une expertise " + methodText + " est requise.\n" +
                        "Un expert sera assigné automatiquement et vous en serez notifié.";
            }

            case PRODUCT_EXPERTISE_PLANNED: {
                String productName = getStringOrDefault(data, "productName", "Produit");
                String dateTime = getStringOrDefault(data, "dateTime", null);
                String method = getStringOrDefault(data, "expertiseMethod", "en ligne");
                Double amount = (Double) data.get("amount");

                StringBuilder msg = new StringBuilder();
                msg.append("Expertise planifiée pour \"").append(productName).append("\"\n\n");

                if (method.equals("en ligne") || method.equals("ONLINE")) {
                    msg.append("Type : Expertise en ligne (à distance)\n");
                    msg.append("L'expert procédera à l'analyse de votre produit en ligne.\n");
                } else {
                    msg.append("Type : Expertise présentielle (sur place)\n");
                    if (dateTime != null) {
                        msg.append("Date du rendez-vous : ").append(formatDateTime(dateTime)).append("\n");
                        msg.append("Lieu : Magasin Bazart\n");
                    }
                }

                if (amount != null) {
                    msg.append("\n Montant : ").append(String.format("%.2f DH", amount));
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

                return " L'enchère est terminée !\n" +
                        " Produit: \"" + productName + "\"\n" +
                        "Gagnant: " + winnerName +
                        (amountStr.isEmpty() ? "" : "\nMontant gagnant: " + amountStr) + "\n" +
                        "Merci pour votre participation !";
            }
            case NEW_BID: {
                String bidUser = (String) data.getOrDefault("bidUserName", "Un utilisateur");
                String bidAmount = String.valueOf(data.getOrDefault("bidAmount", ""));
                String bidProduct = (String) data.getOrDefault("productName", "Produit");

                // 🔥 ASSURER QUE LE PRODUCT ID EST DISPONIBLE
                Long productId = (Long) data.get("productId");
                if (productId == null) {
                    productId = (Long) data.get("produitId");
                }
                if (productId == null) {
                    productId = (Long) data.get("idproduit");
                }

                System.out.println(" Notification NEW_BID - ProductId trouvé dans data: " + productId);

                // Construire le message avec productId intégré
                String baseMessage = bidUser + " a placé une nouvelle enchère de " + bidAmount + " DH sur \"" + bidProduct + "\".";

                // Si on a un productId, l'ajouter au message
                if (productId != null) {
                    String messageWithId = baseMessage + " [productId:" + productId + "]";

                    // 🔥 AJOUTER LE PRODUCT ID COMME DONNÉE SÉPARÉE POUR WEB SOCKET
                    data.put("wsProductId", productId);
                    data.put("extractedProductId", productId);

                    System.out.println(" Message NEW_BID avec productId: " + messageWithId);
                    return messageWithId;
                }

                System.out.println("⚠️ Aucun productId trouvé pour notification NEW_BID");
                return baseMessage;
            }
            case OUTBID:
                String outbidProduct = (String) data.getOrDefault("productName", "Produit");
                return "Vous avez été surenchéri sur \"" + outbidProduct + "\".";
            case AUCTION_WON: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                Object winningAmount = data.getOrDefault("winningAmount", null);
                String amountStr = winningAmount != null ? String.format("%.2f DH", winningAmount) : "";

                return " Félicitations ! Vous avez gagné l'enchère pour \"" + productName + "\"" +
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

                return " l' enchère de votre produit \"" + productName + "\" a été terminéé" +
                        (amountStr.isEmpty() ? " !" : " pour " + amountStr + " !") +
                        "\nLe gagnant "+ winnerName +" va vous contacter pour la livraison.";
            }
            case ADMIN_ALERT: {
                // CORRECTION : Utiliser "message" comme clé principale
                String adminMessage = (String) data.getOrDefault("message", "Message de l'administrateur.");
                String productName = (String) data.getOrDefault("productName", "");
                String alertType = (String) data.getOrDefault("alertType", "PUBLICATION"); // Nouveau: "PUBLICATION" ou "ACHAT"

                StringBuilder msg = new StringBuilder();

                if ("ACHAT".equals(alertType)) {
                    msg.append(" Validation d'achat requise\n");
                    if (!productName.isEmpty()) {
                        msg.append("Produit: \"").append(productName).append("\"\n");
                    }
                    msg.append(adminMessage);
                } else {
                    // Par défaut: PUBLICATION
                    msg.append(" Nouveau produit en attente");
                    if (!productName.isEmpty()) {
                        msg.append(": \"").append(productName).append("\"");
                    }
                    msg.append("\n").append(adminMessage);
                }

                System.out.println(" Notification ADMIN_ALERT créée avec alertType: " + alertType);

                // 🔥 AJOUTER L'alertType AU MESSAGE POUR QU'IL SOIT ACCESSIBLE
                msg.append("\n\n[ALERT_TYPE:").append(alertType).append("]");
                return msg.toString();
            }
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
                String sellerName = (String) data.getOrDefault("sellerName", "le vendeur");
                Object amount = data.getOrDefault("amount", null);
                String amountStr = amount != null ? String.format("%.2f DH", amount) : "";
                String state = (String) data.getOrDefault("state", ""); // Nouveau champ pour différencier

                if ("VENDOR_ACCEPTED".equals(state)) {
                    // Notification pour acheteur quand vendeur accepte
                    return " Le vendeur " + sellerName + " a accepté votre offre !\n" +
                            "Produit: \"" + productName + "\"\n" +
                            (amountStr.isEmpty() ? "" : "Montant: " + amountStr + "\n") +
                            "Veuillez attendre la confirmation d'achat par l'administrateur.";
                } else if ("SOLD".equals(state)) {
                    // Notification pour acheteur quand admin valide l'achat
                    return " Achat confirmé par l'administrateur !\n" +
                            "Votre achat du produit \"" + productName + "\" est maintenant confirmé.\n" +
                            "Le montant a été transmis au vendeur " + sellerName + ".\n" +
                            "Veuillez vous rendre à notre magasin dès que possible pour récupérer votre produit.";
                } else {
                    // Message par défaut
                    return " Transaction acceptée !\n" +
                            "Votre produit \"" + productName + "\" a été accepté par le gagnant " +
                            buyerName + ".\n" +
                            (amountStr.isEmpty() ? "" : "Vous avez été remboursé de " + amountStr + ".\n") +
                            "La transaction est maintenant complète.";
                }
            }
            case TRANSACTION_ANNULEE: {
                System.out.println(" ENTRÉE dans case TRANSACTION_ANNULEE");
                String productName = (String) data.getOrDefault("productName", "Produit");
                String buyerName = (String) data.getOrDefault("buyerName", "un acheteur");
                String sellerName = (String) data.getOrDefault("sellerName", "le vendeur");
                Object amount = data.getOrDefault("amount", null);
                String amountStr = amount != null ? String.format("%.2f DH", amount) : "";
                String state = (String) data.getOrDefault("state", ""); // Nouveau champ pour différencier

                if ("VENDOR_REFUSED".equals(state)) {
                    // Notification pour acheteur quand vendeur refuse
                    return " Le vendeur " + sellerName + " n'a pas accepté votre offre.\n" +
                            "Produit: \"" + productName + "\"\n" +
                            (amountStr.isEmpty() ? "" : "Montant: " + amountStr + "\n") +
                            "La transaction a été annulée.";
                } else if ("ADMIN_CANCELLED".equals(state)) {
                    // Notification pour acheteur quand admin refuse
                    return " Achat refusé par l'administrateur.\n" +
                            "L'achat du produit \"" + productName + "\" a été refusé.\n" +
                            (amountStr.isEmpty() ? "" : "Le montant de " + amountStr + " a été remboursé à votre portefeuille.\n") +
                            "La transaction est maintenant terminée.";
                } else {
                    // Message par défaut
                    return " Transaction annulée !\n" +
                            "La transaction pour votre produit \"" + productName + "\" avec " +
                            buyerName + " a été annulée.\n" +
                            (amountStr.isEmpty() ? "" : "Le montant de " + amountStr + " a été remboursé.") +
                            "La transaction est maintenant terminée.";
                }
            }
            case AUCTION_END_NO_WINNER: {
                String productName = (String) data.getOrDefault("productName", "Produit");
                return " Enchère terminée sans gagnant !\n" +
                        "L'enchère pour votre produit \"" + productName + "\" est terminée, " +
                        "mais aucun participant n'a placé d'offre.\n" +
                        "Vous pouvez relancer l'enchère depuis votre espace vendeur.";
            }
            case MESSAGE:
                String customMessage = (String) data.getOrDefault("message", "Vous avez un nouveau message.");
                return customMessage;
           // case GENERIC:
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