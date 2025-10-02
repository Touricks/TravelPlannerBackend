package org.laioffer.planner.user.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String appBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromEmail,
            @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.appBaseUrl = appBaseUrl;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = appBaseUrl + "/api/auth/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request - Travel Planner");
        message.setText(
            "Hello,\n\n" +
            "You have requested to reset your password for your Travel Planner account.\n\n" +
            "Click the link below to reset your password:\n" +
            resetLink + "\n\n" +
            "This link will expire in 1 hour.\n\n" +
            "If you did not request this password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Travel Planner Team"
        );

        mailSender.send(message);
    }
}
