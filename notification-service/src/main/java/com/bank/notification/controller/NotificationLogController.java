package com.bank.notification.controller;

import com.bank.notification.domain.NotificationLog;
import com.bank.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationLogController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping
    public Page<NotificationLog> listNotifications(Pageable pageable) {
        return notificationLogRepository.findAll(pageable);
    }
}
