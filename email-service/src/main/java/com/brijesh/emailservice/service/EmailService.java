package com.brijesh.emailservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.auth-service-url}")
    private String authServiceUrl;

    // ── Verification Email ────────────────────────────────────────

    public void sendVerificationEmail(String toEmail,
                                      String firstName,
                                      String rawToken) {
        String link = authServiceUrl
                + "/api/v1/auth/verify-email?token="
                + rawToken;

        String body = String.format("""
            Hi %s,

            Welcome to Object Storage System!

            Please verify your email address by clicking the link below:

            %s

            This link expires in 24 hours.

            If you did not create an account, please ignore this email.

            Regards,
            Object Storage System
            """, firstName, link);

        send(toEmail, "Verify your email address", body);
    }

    // ── Password Reset Email ──────────────────────────────────────

    public void sendPasswordResetEmail(String toEmail,
                                       String firstName,
                                       String rawToken) {
        String link = authServiceUrl
                + "/api/v1/auth/reset-password?token="
                + rawToken;

        String body = String.format("""
            Hi %s,

            We received a request to reset your password.

            Click the link below to set a new password:

            %s

            This link expires in 1 hour.

            If you did not request this, please ignore this email.
            Your password will not be changed.

            Regards,
            Object Storage System
            """, firstName, link);

        send(toEmail, "Reset your password", body);
    }

    // ── Welcome Email ─────────────────────────────────────────────

    public void sendWelcomeEmail(String toEmail, String firstName) {
        String body = String.format("""
            Hi %s,

            Your email has been verified successfully!

            You can now login to Object Storage System.

            Regards,
            Object Storage System
            """, firstName);

        send(toEmail, "Welcome to Object Storage System!", body);
    }

    // ── Internal ──────────────────────────────────────────────────

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage());
        }
    }
}
