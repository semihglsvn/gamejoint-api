package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.exception.DuplicateResourceException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitingService rateLimitService;
    private final HttpServletRequest httpRequest;

    @Value("${security.password.pepper}")
    private String pepper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void resendVerificationEmail(String identifier) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyEmailTrigger(ip);

        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (user.getIsVerified() != null && user.getIsVerified()) {
            throw new DuplicateResourceException("This account is already verified! You can just log in.");
        }

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 40px 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden;'>
                    <tr><td align='center' style='padding: 40px 0; background-color: #222222;'><img src='cid:logo_img' alt='GameJoint Logo' width='200' style='display: block;'></td></tr>
                    <tr><td style='padding: 40px;'>
                        <h2 style='color: #333333; margin-top: 0;'>Account Verification</h2>
                        <p style='color: #555555;'>Hello <strong>%s</strong>,</p>
                        <p style='color: #555555;'>Please use the following 6-digit code to verify your account. This code will expire in 15 minutes.</p>
                        <div style='text-align: center; margin: 30px 0;'>
                            <span style='background-color: #f0f0f0; padding: 15px 30px; letter-spacing: 8px; border: 1px solid #dddddd; font-size: 32px; font-weight: bold; border-radius: 4px;'>%s</span>
                        </div>
                    </td></tr>
                </table>
            </div>
            """.formatted(user.getUsername(), otp);

        emailService.sendEmailWithLogo(user.getEmail(), "Verify your GameJoint Account", htmlBody);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String ip = getClientIp(httpRequest);
        
        rateLimitService.verifyPasswordResetAttempt(ip); 

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) return; 

        // ... rest of the method remains exactly the same
        User user = userOptional.get();

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));

        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 40px 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden;'>
                    <tr><td align='center' style='padding: 30px 0; background-color: #1a1a1a;'><img src='cid:logo_img' alt='GameJoint Logo' width='180'></td></tr>
                    <tr><td style='padding: 40px 30px;'>
                        <h2 style='color: #333333; margin-top: 0;'>Password Reset Request</h2>
                        <p style='color: #555555;'>Hello <strong>%s</strong>,</p>
                        <p style='color: #555555;'>We received a request to reset your password. Enter the code below to proceed. This code will expire in 15 minutes.</p>
                        <div style='text-align: center; margin: 30px 0;'>
                            <span style='background-color: #f0f0f0; color: #e74c3c; padding: 15px 30px; letter-spacing: 8px; border: 1px solid #dddddd; font-size: 32px; font-weight: bold; border-radius: 4px;'>%s</span>
                        </div>
                        <p style='color: #999999; font-size: 12px;'>If you did not request a password reset, please ignore this email.</p>
                    </td></tr>
                </table>
            </div>
            """.formatted(user.getUsername(), otp);

        emailService.sendEmailWithLogo(user.getEmail(), "Reset your GameJoint password", htmlBody);
    }

    @Transactional
    public void executePasswordReset(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid request."));

        if (!validateOtp(user, otp)) throw new InvalidCredentialsException("Invalid or expired code.");

        user.setPasswordHash(passwordEncoder.encode(newPassword + pepper));
        
        // Kick everyone out by incrementing the token version
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
    }

    @Transactional
    public void verifyAccount(String identifier, String otp) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (!validateOtp(user, otp)) throw new InvalidCredentialsException("Invalid or expired code.");

        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
    }

    // --- HELPERS ---
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private boolean validateOtp(User user, String providedOtp) {
        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null) return false;
        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) return false;
        return user.getOtpCode().equals(providedOtp);
    }
}