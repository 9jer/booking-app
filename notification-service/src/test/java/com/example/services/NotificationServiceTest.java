package com.example.services;

import com.example.bookingservice.event.BookingCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void listen_ValidEvent_SendsEmail() {
        // Given
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(1L);
        event.setEmail("test@example.com");
        event.setPropertyName("Luxury Villa");
        event.setCheckInDate("2023-01-01");
        event.setCheckOutDate("2023-01-05");

        doNothing().when(mailSender).send(any(MimeMessagePreparator.class));

        // When
        notificationService.listen(event);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
    }

    @Test
    void listen_MailException_ThrowsRuntimeException() {
        // Given
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(1L);
        event.setEmail("test@example.com");

        doThrow(new MailSendException("Mail server error"))
                .when(mailSender).send(any(MimeMessagePreparator.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.listen(event));

        assertTrue(exception.getMessage().contains("Exception occurred while sending mail"));
        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
    }
}