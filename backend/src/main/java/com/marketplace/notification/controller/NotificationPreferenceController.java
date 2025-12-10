package com.marketplace.notification.controller;

import com.marketplace.notification.entity.UserNotificationPreference;
import com.marketplace.notification.service.UserNotificationPreferenceService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-notification-preference")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationPreferenceController {

    private final UserNotificationPreferenceService service;

    @GetMapping("/{userId}")
    public UserNotificationPreference get(@PathVariable Long userId) {
        return service.getOrDefault(userId);
    }

    @PostMapping
    public void set(@RequestBody UserNotificationPreference pref) {
        service.save(pref);
    }
}