package com.bank.notification.channel.impl;

import com.bank.notification.channel.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class LoggingNotificationChannel implements NotificationChannel {

    @Override
    public void send(String recipientEmail, String subject, String message) {
        log.info("notification.delivered channel=log recipient={} subject='{}' body='{}'",
                recipientEmail, subject, message);
    }
}
