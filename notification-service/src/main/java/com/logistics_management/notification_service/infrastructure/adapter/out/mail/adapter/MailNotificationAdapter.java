package com.logistics_management.notification_service.infrastructure.adapter.out.mail.adapter;

import com.logistics_management.notification_service.application.port.out.NotificationSenderPort;
import com.logistics_management.notification_service.domain.exception.NotificationProcessingException;
import com.logistics_management.notification_service.domain.model.Notification;
import com.logistics_management.notification_service.infrastructure.config.NotificationMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailNotificationAdapter implements NotificationSenderPort {
    private final JavaMailSender mailSender;
    private final NotificationMailProperties properties;

    @Override
    public void send(Notification notification) {
        try {
            log.info("email delivery started subject={}", notification.getSubject());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(notification.getRecipient());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody());
            mailSender.send(message);
            log.info("email sent subject={}", notification.getSubject());
        } catch (MailException exception) {
            throw new NotificationProcessingException("Could not send shipment notification", exception);
        }
    }
}
