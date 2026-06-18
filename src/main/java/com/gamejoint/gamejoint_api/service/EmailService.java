package com.gamejoint.gamejoint_api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmailWithLogo(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            
            // The 'true' flag here is critical: it tells Java this email contains attachments/inline files
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setFrom("noreply@gamejoint.com");
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML format
            
            // Replicating your PHP $logo_path = __DIR__ . '/assets/images/logo.png';
            // This grabs the image directly from your Java resources folder!
            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            helper.addInline("logo_img", logo);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to: " + to);
        }
    }
}