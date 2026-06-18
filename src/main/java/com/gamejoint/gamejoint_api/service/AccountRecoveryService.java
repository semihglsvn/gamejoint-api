package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.exception.DuplicateResourceException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // Injected for the final executePasswordReset step
    private final PasswordEncoder passwordEncoder; 

    @Value("${security.password.pepper}")
    private String pepper;

    // ==========================================
    // 1. ACCOUNT VERIFICATION FLOW
    // ==========================================
    
    @Transactional
    public void resendVerificationEmail(String identifier) {
        
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (user.getIsVerified() != null && user.getIsVerified()) {
            throw new DuplicateResourceException("This account is already verified! You can just log in.");
        }

        String verifyToken = UUID.randomUUID().toString();
        user.setVerificationToken(verifyToken);
        
        // Link to local frontend/app deep link
        String verifyLink = "http://localhost:8080/verify?email=" + user.getEmail() + "&token=" + verifyToken;
        String currentYear = String.valueOf(Year.now().getValue());

        // Updated to use the inline cid:logo_img just like the password reset!
        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 40px 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);'>
                    <tr>
                        <td align='center' style='padding: 40px 0; background-color: #222222;'>
                            <img src='cid:logo_img' alt='GameJoint Logo' width='200' style='display: block; border: 0;'>
                        </td>
                    </tr>
                    <tr>
                        <td style='padding: 40px 40px 20px 40px;'>
                            <h2 style='color: #333333; font-size: 24px; margin-top: 0; margin-bottom: 20px;'>New Verification Link</h2>
                            <p style='color: #555555; font-size: 16px; line-height: 1.6; margin-bottom: 20px;'>Hello <strong>%s</strong>,</p>
                            <p style='color: #555555; font-size: 16px; line-height: 1.6; margin-bottom: 30px;'>You requested a new verification link. Click the button below to activate your GameJoint account:</p>
                            
                            <div style='text-align: center; margin: 35px 0;'>
                                <a href='%s' style='background-color: #27ae60; color: #ffffff; padding: 14px 30px; text-decoration: none; border-radius: 4px; font-weight: bold; font-size: 16px; display: inline-block;'>Verify Email Address</a>
                            </div>
                            
                            <p style='color: #777777; font-size: 14px; line-height: 1.6; margin-top: 30px;'>If you didn't request this link, you can safely ignore this email.</p>
                        </td>
                    </tr>
                    <tr>
                        <td align='center' style='padding: 20px 40px; background-color: #f9f9f9; color: #aaaaaa; font-size: 12px; border-top: 1px solid #eeeeee;'>
                            <p style='margin: 0;'>&copy; %s GameJoint. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </div>
            """.formatted(user.getUsername(), verifyLink, currentYear);

        // We use the generic method that attaches the inline logo
        emailService.sendEmailWithLogo(user.getEmail(), "Verify your GameJoint Account", htmlBody);
    }

    // ==========================================
    // 2. PASSWORD RESET FLOW
    // ==========================================

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        // Security best practice: Silent exit if email is not in the database
        if (userOptional.isEmpty()) {
            return; 
        }

        User user = userOptional.get();

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);

        user.setResetTokenHash(hashedToken);
        user.setResetTokenExpires(LocalDateTime.now().plusMinutes(15));

        String resetLink = "http://localhost:8080/reset_password?token=" + rawToken + "&email=" + user.getEmail();
        String currentYear = String.valueOf(Year.now().getValue());

        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1);'>
                    <tr>
                        <td align='center' style='padding: 30px 0; background-color: #1a1a1a;'>
                            <img src='cid:logo_img' alt='GameJoint Logo' width='180' style='display: block; border: 0;'>
                        </td>
                    </tr>
                    <tr>
                        <td style='padding: 40px 30px;'>
                            <h1 style='color: #333333; font-size: 22px; margin-top: 0;'>Password Reset Request</h1>
                            <p style='color: #555555; font-size: 16px; line-height: 1.6;'>Hello <strong>%s</strong>,</p>
                            <p style='color: #555555; font-size: 16px; line-height: 1.6;'>We received a request to reset your password. Click the button below to get started:</p>
                            
                            <div style='text-align: center; margin: 30px 0;'>
                                <a href='%s' style='background-color: #27ae60; color: #ffffff; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;'>Reset Password</a>
                            </div>
                            
                            <p style='color: #888888; font-size: 14px; line-height: 1.6;'>For security, this link will expire in <strong>15 minutes</strong>. If you didn't request this, you can safely ignore this email.</p>
                        </td>
                    </tr>
                    <tr>
                        <td align='center' style='padding: 20px; background-color: #f9f9f9; color: #999999; font-size: 12px;'>
                            <p style='margin: 0;'>&copy; %s GameJoint. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </div>
            """.formatted(user.getUsername(), resetLink, currentYear);

        emailService.sendEmailWithLogo(user.getEmail(), "Reset your GameJoint password", htmlBody);
    }

    @Transactional
    public void executePasswordReset(String rawToken, String newPassword) {
        
        String hashedToken = hashToken(rawToken);

        User user = userRepository.findByResetTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token."));

        if (user.getResetTokenExpires() == null || user.getResetTokenExpires().isBefore(LocalDateTime.now())) {
            user.setResetTokenHash(null);
            user.setResetTokenExpires(null);
            throw new InvalidCredentialsException("This reset link has expired. Please request a new one.");
        }

        String pepperedPassword = newPassword + pepper;
        user.setPasswordHash(passwordEncoder.encode(pepperedPassword));

        user.setResetTokenHash(null);
        user.setResetTokenExpires(null);
    }

    // ==========================================
    // 3. PRIVATE SECURITY HELPERS
    // ==========================================

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}