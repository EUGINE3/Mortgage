package com.bank.notification.channel.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(mailSender);
        ReflectionTestUtils.setField(channel, "fromAddress", "noreply@bank.com");
    }

    @Test
    void send_buildsAndDispatchesMailMessage() {
        channel.send("user@example.com", "Test Subject", "Test body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@bank.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Test Subject");
        assertThat(message.getText()).isEqualTo("Test body");
    }
}
