package com.brijesh.authservice.infrastructure.email;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Email Verification

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String rawToken){
        String verificationLink = "http://localhost:8080/api/v1/auth/verify-email?token="+rawToken;
        String subject = "Verify your email address";
        String body = String.format("""
                Hi %s,
                
                Welcome! Please verify your email address by clicking the link below:
                
                %s
                
                This link expires in 24 hours.
                
                If you did not create an account, please ignore this email.
                
                Regards,
                Object Storage System
                
                """, firstName,verificationLink);
        sendEmail(toEmail,subject,body);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String rawToken){
        String resetLink = "http://localhost:8080/api/v1/auth/reset-password?token="+rawToken;
        String subject = "Reset your password";
        String body = String.format("""
                Hi %s,
                
                We received a request to reset your password.
                Click the link below to set a new password:
                
                %s
                
                This link expires in 1 hour.
                
                If you did not request a password reset, please ignore this email.
                Your password will not be changed.
                
                Regards,
                Object Storage system
                
                """, firstName,resetLink);
        sendEmail(toEmail,subject,body);
    }

    private void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}",toEmail);
        } catch (Exception e){
            log.error("Failed to send email to {}: {}",toEmail, e.getMessage());
        }
    }
}
