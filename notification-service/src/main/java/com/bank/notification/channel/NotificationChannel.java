package com.bank.notification.channel;

public interface NotificationChannel {

    void send(String recipientEmail, String subject, String message);
}
