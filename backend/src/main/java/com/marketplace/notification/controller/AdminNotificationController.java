// AdminNotificationController.java
package com.marketplace.notification.controller;

import com.marketplace.notification.entity.Notification;
import com.marketplace.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@CrossOrigin(origins = "*")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{adminId}")
    public ResponseEntity<List<Notification>> getAdminNotifications(@PathVariable Long adminId) {
        try {
            System.out.println("👑 [BACKEND] Récupération notifications admin ID: " + adminId);

            // Même méthode que pour les users, mais accès différent
            List<Notification> notifications = notificationService.getUserNotifications(adminId);

            System.out.println("📊 [BACKEND] Notifications admin trouvées: " + notifications.size());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            System.err.println("❌ [BACKEND] Erreur récupération notifications admin: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/read/{notificationId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            System.out.println("👑 [BACKEND] Marquer notification admin comme lue: " + notificationId);
            notificationService.markAsRead(notificationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ [BACKEND] Erreur marquer notification admin comme lue: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}