package com.marketplace.notification.service;

import com.marketplace.notification.entity.UserNotificationPreference;
import com.marketplace.notification.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceService {

    private final UserNotificationPreferenceRepository repository;

    /**
     * Retourne la préférence si existante, sinon retourne une préférence par défaut (in-app activé).
     */
    public UserNotificationPreference getOrDefault(Long userId) {
        return repository.findByUserId(userId)
                .orElse(UserNotificationPreference.builder()
                        .userId(userId)
                        .inAppEnabled(true)
                        .emailEnabled(false)
                        .build());
    }
    /**
     * Crée ou met à jour la préférence de notification pour un utilisateur.
     */
    public void save(UserNotificationPreference pref) {
        // Vérifier s'il existe déjà une préférence pour cet utilisateur
        UserNotificationPreference existing = repository.findByUserId(pref.getUserId()).orElse(null);
        if (existing != null) {
            existing.setInAppEnabled(pref.isInAppEnabled());
            existing.setEmailEnabled(pref.isEmailEnabled());
            repository.save(existing);
        } else {
            repository.save(pref);
        }
    }
}