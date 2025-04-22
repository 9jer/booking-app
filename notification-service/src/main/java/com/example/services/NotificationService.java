package com.example.services;

import com.example.bookingservice.event.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @KafkaListener(topics = "booking-created")
    public void listen(BookingCreatedEvent bookingCreatedEvent) {
        log.info("Received from booking-created topic: {}", bookingCreatedEvent);
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("springbooking@email.com");
            messageHelper.setTo(bookingCreatedEvent.getEmail());
            messageHelper.setSubject(String.format("Booking №%s successfully created", bookingCreatedEvent.getBookingId()));
            messageHelper.setText(String.format("""
                    Hello!
                                        
                    Your booking has been successfully created. Here are the details:
                                        
                    📌   Booking ID: %d
                    🏠   Property name: %s
                    📅   Stay period: from %s to %s 
                                        
                    If you have any questions, feel free to contact our support team.
                                        
                    Have a great day!
                    """,
                    bookingCreatedEvent.getBookingId(),
                    bookingCreatedEvent.getPropertyName(),
                    bookingCreatedEvent.getCheckInDate().toString(),
                    bookingCreatedEvent.getCheckOutDate().toString()
            ));
        };

        try {
            mailSender.send(messagePreparator);
            log.info("Booking Email Notification has been sent.");
        } catch (MailException e) {
            log.error("Exception occurred while sending mail.", e);
            throw new RuntimeException(String.format("Exception occurred while sending mail to %s", bookingCreatedEvent.getEmail()), e);
        }


    }
}
