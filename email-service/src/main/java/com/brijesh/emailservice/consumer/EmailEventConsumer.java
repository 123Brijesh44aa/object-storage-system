package com.brijesh.emailservice.consumer;


import com.brijesh.emailservice.config.KafkaTopics;
import com.brijesh.emailservice.event.EmailVerificationRequestEvent;
import com.brijesh.emailservice.event.PasswordResetRequestedEvent;
import com.brijesh.emailservice.event.UserRegisteredEvent;
import com.brijesh.emailservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventConsumer {

    private final EmailService emailService;

    // User Registered
    @KafkaListener(
            topics = KafkaTopics.USER_REGISTERED,
            groupId = "email-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(
            @Payload UserRegisteredEvent event,
            @Header (KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received {} | topic={} partition={} offset={}", event.getEventType(), topic, partition, offset);

        try {
            emailService.sendVerificationEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getVerificationToken()
            );
        } catch (Exception e) {
         log.error("Failed to handle USER_REGISTERED for {}: {}", event.getEmail(), e.getMessage());
        }
    }

    // ── Email Verification Requested ──────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.EMAIL_VERIFICATION_REQUESTED,
            groupId = "email-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleEmailVerificationRequested(
            @Payload EmailVerificationRequestEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received {} | topic={} partition={} offset={}",
                event.getEventType(), topic, partition, offset);

        try {
            emailService.sendVerificationEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getVerificationToken()
            );
        } catch (Exception e) {
            log.error("Failed to handle EMAIL_VERIFICATION_REQUESTED for {}: {}",
                    event.getEmail(), e.getMessage());
        }
    }



    // ── Password Reset Requested ──────────────────────────────────

    @KafkaListener(
            topics = KafkaTopics.PASSWORD_RESET_REQUESTED,
            groupId = "email-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePasswordResetRequested(
            @Payload PasswordResetRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received {} | topic={} partition={} offset={}",
                event.getEventType(), topic, partition, offset);

        try {
            emailService.sendPasswordResetEmail(
                    event.getEmail(),
                    event.getFirstName(),
                    event.getResetToken()
            );
        } catch (Exception e) {
            log.error("Failed to handle PASSWORD_RESET_REQUESTED for {}: {}",
                    event.getEmail(), e.getMessage());
        }
    }
}
