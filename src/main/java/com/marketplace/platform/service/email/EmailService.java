package com.marketplace.platform.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.email.support")
    private String supportEmail;

    public void sendWelcomeEmail(String to, String firstName) throws MessagingException {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
        context.setVariable("helpUrl", frontendUrl + "/help");
        context.setVariable("socialLinks", getSocialLinks());

        sendEmail(to,
                "Welcome to Our Marketplace!",
                "email/welcome-email",
                context);
    }

    public void sendVerificationEmail(String to, String token, String firstName) throws MessagingException {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("verificationUrl", frontendUrl + "/verify?token=" + token);

        sendEmail(to,
                "Please verify your email address",
                "email/verification-email",
                context);
    }

    public void sendPasswordResetEmail(String to, String token, String firstName) throws MessagingException {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + token);

        sendEmail(to,
                "Password Reset Request",
                "email/password-reset-email",
                context);
    }

    public void sendPasswordChangeNotification(String to, String firstName) throws MessagingException {
        Context context = new Context();
        context.setVariable("firstName", firstName);

        sendEmail(to,
                "Password Changed Successfully",
                "email/password-change-email",
                context);
    }

    public void sendStatusChangeNotification(String to, String firstName, String status) throws MessagingException {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("status", status);

        sendEmail(to,
                "Account Status Update",
                "email/status-change-email",
                context);
    }

    public void sendAccountDeletionEmail(String to, String firstName) throws MessagingException {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("signupUrl", frontendUrl + "/signup");

        sendEmail(to,
                "Account Deletion Confirmation",
                "email/account-deleted-email",
                context);
    }

    private Map<String, String> getSocialLinks() {
        Map<String, String> links = new HashMap<>();
        links.put("facebook", "https://facebook.com/yourmarketplace");
        links.put("twitter", "https://twitter.com/yourmarketplace");
        links.put("instagram", "https://instagram.com/yourmarketplace");
        return links;
    }

    private void sendEmail(String to, String subject, String templateName, Context context) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw e;
        }
    }
}