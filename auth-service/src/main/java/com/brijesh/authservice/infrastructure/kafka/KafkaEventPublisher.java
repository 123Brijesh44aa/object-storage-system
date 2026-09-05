package com.brijesh.authservice.infrastructure.kafka;


import com.brijesh.authservice.config.KafkaTopics;
import com.brijesh.authservice.event.BaseEvent;
import com.brijesh.authservice.event.EmailVerificationRequestedEvent;
import com.brijesh.authservice.event.PasswordResetRequestedEvent;
import com.brijesh.authservice.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String,Object> kafkaTemplate;

    public void publishUserRegistered(String userUuid, String email, String firstName, String verificationToken){
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventType("USER_REGISTERED")
                .userUuid(userUuid)
                .email(email)
                .firstName(firstName)
                .verificationToken(verificationToken)
                .build();

        publish(KafkaTopics.USER_REGISTERED, userUuid, event);
    }

    public void publishEmailVerificationRequested(String userUuid, String email, String firstName, String verificationToken){
        log.info("Publishing email verification for {} with token: {}",
                email, verificationToken);
        EmailVerificationRequestedEvent event = EmailVerificationRequestedEvent.builder()
                .eventType("EMAIL_VERIFICATION_REQUESTED")
                .userUuid(userUuid)
                .email(email)
                .firstName(firstName)
                .verificationToken(verificationToken)
                .build();

        log.info("Event verificationToken field: {}",
                event.getVerificationToken());

        publish(KafkaTopics.EMAIL_VERIFICATION_REQUESTED, userUuid, event);
    }

    public void publishPasswordResetRequested(String userUuid, String email, String firstName, String resetToken) {
        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.builder()
                .eventType("PASSWORD_RESET_REQUESTED")
                .userUuid(userUuid)
                .email(email)
                .firstName(firstName)
                .resetToken(resetToken)
                .build();

        publish(KafkaTopics.PASSWORD_RESET_REQUESTED, userUuid, event);
    }


    private void publish(String topic, String key, BaseEvent event) {
        kafkaTemplate.send(topic,key,event)
                .whenComplete((result,ex) -> {
                   if (ex != null){
                       log.error("Failed to publish event {} to topic {}: {}",event.getEventType(),topic,ex.getMessage());
                   } else {
                       log.info("Published event {} to topic {} partition {} offset {}",
                               event.getEventType(),
                               topic,
                               result.getRecordMetadata(),
                               result.getRecordMetadata().offset());
                   }
                });
    }


}
























