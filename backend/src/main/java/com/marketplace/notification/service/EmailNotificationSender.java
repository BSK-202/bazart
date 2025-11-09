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

    @Override
    public void send(Notification notification, Long userId, String templateMessage) {
        System.out.println("Appel EmailNotificationSender.send pour userId=" + userId); // <--- DEBUG

        clientRepository.findEmailById(userId).ifPresentOrElse(email -> {
            System.out.println("Email trouvé pour userId=" + userId + " = " + email); // <--- DEBUG
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(email);
                msg.setSubject("Bazart - Nouvelle notification");
                msg.setText(templateMessage);
                msg.setFrom(fromAddress);
                mailSender.send(msg);
                System.out.println("Email envoyé à " + email + " pour userId=" + userId);
            } catch (Exception ex) {
                System.err.println("Erreur envoi email à " + email + " : " + ex.getMessage());
            }
        }, () -> System.out.println("Aucun email trouvé pour userId=" + userId));
    }

    @Override
    public boolean supportsChannel(String channel) {
        return "EMAIL".equalsIgnoreCase(channel);
    }
}