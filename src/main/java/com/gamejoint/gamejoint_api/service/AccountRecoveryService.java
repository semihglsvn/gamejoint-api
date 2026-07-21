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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

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

        String verifyToken = UUID.randomUUID().toString();
        user.setVerificationToken(verifyToken);
        String verifyLink = "http://localhost:8080/verify?email=" + user.getEmail() + "&token=" + verifyToken;

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        String currentYear = String.valueOf(Year.now().getValue());

        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 40px 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden;'>
                    <tr><td align='center' style='padding: 40px 0; background-color: #222222;'><img src='cid:logo_img' alt='GameJoint Logo' width='200' style='display: block;'></td></tr>
                    <tr><td style='padding: 40px;'>
                        <h2 style='color: #333333; margin-top: 0;'>Account Verification</h2>
                        <p style='color: #555555;'>Hello <strong>%s</strong>,</p>
                        
                        <p style='color: #555555; font-weight: bold;'>If you are using the Mobile App, enter this code:</p>
                        <div style='text-align: center; margin: 20px 0;'>
                            <span style='background-color: #f0f0f0; padding: 15px 30px; letter-spacing: 5px; border: 1px solid #dddddd; font-size: 28px; font-weight: bold;'>%s</span>
                        </div>
                        
                        <hr style='border: 1px solid #eeeeee; margin: 30px 0;'/>
                        
                        <p style='color: #555555; font-weight: bold;'>If you are using the Website, click here:</p>
                        <div style='text-align: center; margin: 20px 0;'>
                            <a href='%s' style='background-color: #27ae60; color: #ffffff; padding: 14px 30px; text-decoration: none; border-radius: 4px; font-weight: bold;'>Verify Email Address</a>
                        </div>
                    </td></tr>
                </table>
            </div>
            """.formatted(user.getUsername(), otp, verifyLink);

        emailService.sendEmailWithLogo(user.getEmail(), "Verify your GameJoint Account", htmlBody);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyEmailTrigger(ip);

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) return; 

        User user = userOptional.get();

        String rawToken = UUID.randomUUID().toString();
        user.setResetTokenHash(hashToken(rawToken));
        user.setResetTokenExpires(LocalDateTime.now().plusMinutes(15));
        String resetLink = "http://localhost:8080/reset_password?token=" + rawToken + "&email=" + user.getEmail();

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));

        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px;'>
                    <tr><td align='center' style='padding: 30px 0; background-color: #1a1a1a;'><img src='cid:logo_img' alt='GameJoint Logo' width='180'></td></tr>
                    <tr><td style='padding: 40px 30px;'>
                        <h1 style='color: #333333; margin-top: 0;'>Password Reset Request</h1>
                        <p style='color: #555555;'>Hello <strong>%s</strong>,</p>
                        
                        <p style='color: #555555; font-weight: bold;'>Mobile App Users - Enter this code:</p>
                        <div style='text-align: center; margin: 20px 0;'>
                            <span style='background-color: #f0f0f0; color: #e74c3c; padding: 15px 30px; letter-spacing: 5px; border: 1px solid #dddddd; font-size: 28px; font-weight: bold;'>%s</span>
                        </div>
                        
                        <hr style='border: 1px solid #eeeeee; margin: 30px 0;'/>
                        
                        <p style='color: #555555; font-weight: bold;'>Website Users - Click here:</p>
                        <div style='text-align: center; margin: 20px 0;'>
                            <a href='%s' style='background-color: #27ae60; color: #ffffff; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Reset Password</a>
                        </div>
                    </td></tr>
                </table>
            </div>
            """.formatted(user.getUsername(), otp, resetLink);

        emailService.sendEmailWithLogo(user.getEmail(), "Reset your GameJoint password", htmlBody);
    }

    // --- WEB SPECIFIC EXECUTIONS ---
    @Transactional
    public void executePasswordReset(String rawToken, String newPassword) {
        String hashedToken = hashToken(rawToken);
        User user = userRepository.findByResetTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token."));

        if (user.getResetTokenExpires() == null || user.getResetTokenExpires().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("This reset link has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword + pepper));
        
        // --- NEW: KICK EVERYONE OUT ---
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        
        user.setResetTokenHash(null);
        user.setResetTokenExpires(null);
        user.setOtpCode(null); 
        user.setOtpExpiresAt(null);
    }

    // --- MOBILE SPECIFIC EXECUTIONS ---
    @Transactional
    public void executePasswordResetOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid request."));

        if (!validateOtp(user, otp)) throw new InvalidCredentialsException("Invalid or expired code.");

        user.setPasswordHash(passwordEncoder.encode(newPassword + pepper));
        
        // --- NEW: KICK EVERYONE OUT ---
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setResetTokenHash(null); 
        user.setResetTokenExpires(null);
    }

    @Transactional
    public void verifyAccountOtp(String identifier, String otp) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (!validateOtp(user, otp)) throw new InvalidCredentialsException("Invalid or expired code.");

        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setVerificationToken(null);
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

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}