package com.marketplace.notification.service;

import com.marketplace.notification.entity.Notification;
import com.marketplace.user.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final ClientRepository clientRepository;

    @Value("${app.mail.from:no-reply@bazart.com}")
    private String fromAddress;

    @Value("${app.mail.reply.to:}")
    private String replyToAddress;

    @Override
    public void send(Notification notification, Long userId, String templateMessage) {
        clientRepository.findEmailById(userId).ifPresent(email -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(email);
                msg.setSubject("Bazart - Nouvelle notification");
                msg.setText(templateMessage);

                // Définit l'expéditeur (visible aux destinataires)
                msg.setFrom(fromAddress);

                // Optionnel : définit un Reply-To si tu veux que les réponses aillent autre part
                if (replyToAddress != null && !replyToAddress.isBlank()) {
                    msg.setReplyTo(replyToAddress);
                }

                mailSender.send(msg);
                System.out.println("Email envoyé à " + email + " pour userId=" + userId);
            } catch (Exception ex) {
                // Remplacer par logger.error en production
                System.err.println("Erreur envoi email à " + email + " : " + ex.getMessage());
            }
        });
    }

    @Override
    public boolean supportsChannel(String channel) {
        return "EMAIL".equalsIgnoreCase(channel);
    }
}