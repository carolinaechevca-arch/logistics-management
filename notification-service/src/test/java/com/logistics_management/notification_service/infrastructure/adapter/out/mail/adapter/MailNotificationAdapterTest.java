package com.logistics_management.notification_service.infrastructure.adapter.out.mail.adapter;

import com.logistics_management.notification_service.domain.exception.NotificationProcessingException;
import com.logistics_management.notification_service.domain.model.Notification;
import com.logistics_management.notification_service.infrastructure.config.NotificationMailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailNotificationAdapterTest {
    @Mock JavaMailSender mailSender;
    @Mock NotificationMailProperties properties;
    @InjectMocks MailNotificationAdapter adapter;

    @Test
    void buildsAndSendsMailMessage() {
        Notification notification = new Notification("customer@example.com", "Shipment created", "Body");
        when(properties.getFrom()).thenReturn("noreply@logistics.com");

        adapter.send(notification);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@logistics.com");
        assertThat(message.getTo()).containsExactly("customer@example.com");
        assertThat(message.getSubject()).isEqualTo("Shipment created");
        assertThat(message.getText()).isEqualTo("Body");
    }

    @Test
    void wrapsMailProviderFailure() {
        Notification notification = new Notification("customer@example.com", "Shipment created", "Body");
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> adapter.send(notification))
                .isInstanceOf(NotificationProcessingException.class)
                .hasMessage("Could not send shipment notification")
                .hasCause(failure);
    }
}
