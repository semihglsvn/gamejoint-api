package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.AccountDeleteRequest;
import com.gamejoint.gamejoint_api.dto.EmailChangeRequest;
import com.gamejoint.gamejoint_api.dto.PasswordChangeRequest;
import com.gamejoint.gamejoint_api.dto.PublicProfileResponse;
import com.gamejoint.gamejoint_api.dto.ReviewResponse;
import com.gamejoint.gamejoint_api.dto.UserProfileResponse;
import com.gamejoint.gamejoint_api.exception.DuplicateResourceException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RateLimitingService rateLimitService;
    private final HttpServletRequest httpRequest;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.password.pepper}")
    private String pepper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setDob(user.getDob());
        response.setIsVerified(user.getIsVerified());
        response.setIsBanned(user.getIsBanned());
        response.setBanExpiresAt(user.getBanExpiresAt());
        response.setDeletionDate(user.getDeletionScheduledAt());
        
        // --- ADD THIS LINE ---
        response.setCreatedAt(user.getCreatedAt());
        
        if (user.getRole() != null) {
            response.setRoleName(user.getRole().getRoleName());
        }

        return response;
    }
 // ==========================================
    // PUBLIC PROFILE (Safe Lookup)
    // ==========================================
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PublicProfileResponse response = new PublicProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setCreatedAt(user.getCreatedAt());
        response.setIsBanned(user.getIsBanned());
        if (user.getRole() != null) response.setRoleName(user.getRole().getRoleName());
        
        return response;
    }
    // ==========================================
    // OTP GENERATION (Settings Guard)
    // ==========================================

    
    @Transactional
    public void requestSettingsOtp(Long userId) {
        // 1. Prevent email spam!
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyEmailTrigger(ip);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        String htmlBody = """
            <div style='background-color: #f4f4f4; padding: 40px 20px; font-family: Arial, sans-serif;'>
                <table align='center' border='0' cellpadding='0' cellspacing='0' width='600' style='background-color: #ffffff; border-radius: 8px;'>
                    <tr><td align='center' style='padding: 40px 0; background-color: #1a1a1a;'><img src='cid:logo_img' width='180'></td></tr>
                    <tr><td style='padding: 40px 30px;'>
                        <h2 style='color: #333333; margin-top: 0;'>Account Security Request</h2>
                        <p style='color: #555555;'>Hello <strong>%s</strong>,</p>
                        <p style='color: #555555;'>You recently requested to make a sensitive change to your GameJoint account settings. Enter this code in the app to proceed:</p>
                        <div style='text-align: center; margin: 20px 0;'>
                            <span style='background-color: #f0f0f0; color: #e74c3c; padding: 15px 30px; letter-spacing: 5px; border: 1px solid #dddddd; font-size: 28px; font-weight: bold;'>%s</span>
                        </div>
                        <p style='color: #777777; font-size: 12px;'>If you did not request this, please change your password immediately.</p>
                    </td></tr>
                </table>
            </div>
            """.formatted(user.getUsername(), otp);

        emailService.sendEmailWithLogo(user.getEmail(), "GameJoint Settings Verification Code", htmlBody);
    }

    // ==========================================
    // SENSITIVE ACTIONS (OTP Gated)
    // ==========================================
    @Transactional
    public void changeEmail(Long userId, EmailChangeRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        validateSettingsOtp(user, request.getOtpCode());

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new DuplicateResourceException("That email is already registered to another account.");
        }

        user.setEmail(request.getNewEmail());
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1); // Kill Switch!
        clearOtp(user);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        validateSettingsOtp(user, request.getOtpCode());

        String pepperedNew = request.getNewPassword() + pepper;
        user.setPasswordHash(passwordEncoder.encode(pepperedNew));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1); // Kill Switch!
        clearOtp(user);
    }

 // ==========================================
    // ACCOUNT DELETION LOGIC
    // ==========================================

    @Transactional
    public void deleteAccount(Long userId, AccountDeleteRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        validateSettingsOtp(user, request.getOtpCode());

        // 1. Instead of deleting immediately, set the timer for 7 days from now
        user.setDeletionScheduledAt(LocalDateTime.now().plusDays(7));
        
        // 2. Increment token version to immediately log them out of all devices!
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        clearOtp(user);
        
        userRepository.save(user);
    }

    @Transactional
    public void cancelAccountDeletion(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        // Instantly clears the scheduled deletion
        user.setDeletionScheduledAt(null);
        userRepository.save(user);
    }

    // ==========================================
    // AUTOMATED DATABASE SWEEPER
    // ==========================================

    // Cron format: Seconds Minutes Hours DayOfMonth Month DayOfWeek
    // "0 59 23 * * ?" = Runs at exactly 23:59:00 every single day
    @Scheduled(cron = "0 59 23 * * ?") 
    @Transactional
    public void processScheduledDeletions() {
        // Find everyone whose 7 days are up
        List<User> usersToDelete = userRepository.findByDeletionScheduledAtBefore(LocalDateTime.now());
        
        if (!usersToDelete.isEmpty()) {
            // Physically deletes the users. 
            // Make sure your MariaDB Foreign Keys have ON DELETE CASCADE for their reviews/reports!
            userRepository.deleteAll(usersToDelete);
            System.out.println("Automated Sweep: Permanently deleted " + usersToDelete.size() + " accounts.");
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================
    private void validateSettingsOtp(User user, String providedOtp) {
        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null ||
            user.getOtpExpiresAt().isBefore(LocalDateTime.now()) ||
            !user.getOtpCode().equals(providedOtp)) {
            throw new InvalidCredentialsException("Invalid or expired OTP code.");
        }
    }

    private void clearOtp(User user) {
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}