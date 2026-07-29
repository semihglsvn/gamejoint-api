package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.UserLoginRequest;
import com.gamejoint.gamejoint_api.dto.UserRegistrationRequest;
import com.gamejoint.gamejoint_api.exception.AccountRestrictedException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.UserAlreadyExistsException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RestTemplate restTemplate = new RestTemplate();
	private final AccountRecoveryService accountRecoveryService;
	private final LoginNotificationService loginNotificationService;
	// Inject the HttpServletRequest to check for headers
	private final HttpServletRequest httpRequest;
	private final RateLimitingService rateLimitService; // <--- INJECT IT
	@Value("${cloudflare.turnstile.secret}")
	private String turnstileSecret;

	@Value("${mobile.api.secret}")
	private String mobileApiSecret; // Add this to your application.properties!

	@Transactional
    public void register(UserRegistrationRequest request) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyRegistrationAttempt(ip);

        try {
            verifyTurnstile(request.getCfTurnstileResponse());

            if (userRepository.existsByUsername(request.getUsername())
                    || userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Username or Email is already taken.");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setDob(request.getDob());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

            user.setIsVerified(false);
            user.setIsBanned(false);
            user.setFalseReportStrikes(0);
            user.setShadowbannedReports(false);

            userRepository.save(user);

            accountRecoveryService.resendVerificationEmail(user.getEmail());
            
        } catch (Exception e) {
            // Refund the rate-limit token so failed attempts or server errors don't lock out the user
            rateLimitService.refundRegistrationAttempt(ip);
            throw e; // Re-throw the exception so the global handler still returns the proper error response
        }
    }
	public String login(UserLoginRequest request) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyLoginAttempt(ip);

        verifyTurnstile(request.getCfTurnstileResponse());

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No account found with that username or email."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password.");
        }

        if (user.getIsVerified() != null && !user.getIsVerified()) {
            throw new AccountRestrictedException("Your account is not verified. Please check your email.");
        }

        // --- FIXED: BAN EXPIRATION CHECK ---
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            if (user.getBanExpiresAt() != null && user.getBanExpiresAt().isBefore(LocalDateTime.now())) {
                // Ban expired! Lift it before generating the token.
                user.setIsBanned(false);
                user.setBanExpiresAt(null);
                userRepository.save(user); 
            } else {
                throw new AccountRestrictedException("Your account is currently suspended until " + 
                    (user.getBanExpiresAt() != null ? user.getBanExpiresAt().toString() : "forever."));
            }
        }

        String realIpAddress = getClientIp(httpRequest);
        loginNotificationService.sendNewLoginAlert(user.getEmail(), realIpAddress, user.getUsername());

        return jwtService.generateToken(user);
    }

	// ==========================================
	// HELPER: GET REAL IP BEHIND REVERSE PROXY
	// ==========================================
	private String getClientIp(HttpServletRequest request) {
		// 1. Cloudflare's specific header
		String ip = request.getHeader("CF-Connecting-IP");

		// 2. Standard proxy header (if Cloudflare is off but Nginx is on)
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Forwarded-For");
		}

		// 3. Fallback to the direct connection (Will be 127.0.0.1 if behind tunnel)
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		// X-Forwarded-For can return multiple IPs (client, proxy1, proxy2).
		// The first one is always the true client.
		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0].trim();
		}

		return ip;
	}

	private void verifyTurnstile(String cfResponse) {
		// --- MOBILE APP BYPASS ---
		String clientSecretHeader = httpRequest.getHeader("X-Mobile-App-Secret");
		if (clientSecretHeader != null && clientSecretHeader.equals(mobileApiSecret)) {
			return; // Skip Cloudflare entirely for authentic native app requests
		}

		// --- STANDARD WEB VERIFICATION ---
		if (cfResponse == null || cfResponse.isBlank()) {
			throw new InvalidCredentialsException("Security widget failed to load. Please try again.");
		}

		String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

		var request = Map.of("secret", turnstileSecret, "response", cfResponse);

		@SuppressWarnings("unchecked")
		Map<String, Object> body = restTemplate.postForObject(url, request, Map.class);

		if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
			throw new InvalidCredentialsException("Cloudflare verification failed. Are you a bot?");
		}
	}
}