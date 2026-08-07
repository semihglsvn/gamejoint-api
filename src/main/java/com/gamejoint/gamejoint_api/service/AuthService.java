package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.*;
import com.gamejoint.gamejoint_api.exception.AccountRestrictedException;
import com.gamejoint.gamejoint_api.exception.BadRequestException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.UserAlreadyExistsException;
import com.gamejoint.gamejoint_api.model.LinkedAccount;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.LinkedAccountRepository;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AccountRecoveryService accountRecoveryService;
    private final LoginNotificationService loginNotificationService;
    private final HttpServletRequest httpRequest;
    private final RateLimitingService rateLimitService; 

    @Value("${cloudflare.turnstile.secret}")
    private String turnstileSecret;

    @Value("${mobile.api.secret}")
    private String mobileApiSecret; 

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${security.password.pepper}")
    private String pepper;

    // ==========================================
    // STANDARD REGISTRATION & LOGIN
    // ==========================================

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
            
            // --- PEPPER APPLIED HERE ---
            user.setPasswordHash(passwordEncoder.encode(request.getPassword() + pepper));

            user.setIsVerified(false);
            user.setIsBanned(false);
            user.setFalseReportStrikes(0);
            user.setShadowbannedReports(false);

            userRepository.save(user);

            accountRecoveryService.resendVerificationEmail(user.getEmail());
            
        } catch (Exception e) {
            rateLimitService.refundRegistrationAttempt(ip);
            throw e; 
        }
    }

    public String login(UserLoginRequest request) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyLoginAttempt(ip);

        verifyTurnstile(request.getCfTurnstileResponse());

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No account found with that username or email."));

        // --- PEPPER APPLIED HERE ---
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword() + pepper, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password.");
        }

        validateUserStatus(user);

        loginNotificationService.sendNewLoginAlert(user.getEmail(), ip, user.getUsername());

        return jwtService.generateToken(user);
    }

    // ==========================================
    // OAUTH FLOWS (UNIVERSAL)
    // ==========================================

    @Transactional
    public OAuthAuthResponse oauthLogin(OAuthLoginRequest request) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyLoginAttempt(ip);
        verifyTurnstile(request.getCfTurnstileResponse());

        // 1. Verify the token with the external provider
        ProviderUserInfo userInfo = verifyProviderToken(request.getProvider(), request.getProviderToken());

        // 2. Check if this specific provider account is already linked
        Optional<LinkedAccount> linkedAccountOpt = linkedAccountRepository.findByProviderAndProviderId(request.getProvider(), userInfo.providerId());
        
        if (linkedAccountOpt.isPresent()) {
            User user = linkedAccountOpt.get().getUser();
            validateUserStatus(user);
            loginNotificationService.sendNewLoginAlert(user.getEmail(), ip, user.getUsername());
            
            return OAuthAuthResponse.builder()
                    .isNewUser(false)
                    .jwtToken(jwtService.generateToken(user))
                    .build();
        }

        // 3. Auto-Link: If no LinkedAccount exists, but the verified email matches an existing user
        Optional<User> userOpt = userRepository.findByEmail(userInfo.email());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            validateUserStatus(user);
            
            LinkedAccount newLink = new LinkedAccount(user, request.getProvider(), userInfo.providerId());
            linkedAccountRepository.save(newLink);
            
            loginNotificationService.sendNewLoginAlert(user.getEmail(), ip, user.getUsername());
            
            return OAuthAuthResponse.builder()
                    .isNewUser(false)
                    .jwtToken(jwtService.generateToken(user))
                    .build();
        }

        // 4. Brand New User: Return payload instructing frontend to ask for Username & DOB
        return OAuthAuthResponse.builder()
                .isNewUser(true)
                .email(userInfo.email())
                .jwtToken(null)
                .build();
    }

    @Transactional
    public String completeOAuthRegistration(OAuthRegistrationCompleteRequest request) {
        String ip = getClientIp(httpRequest);
        rateLimitService.verifyRegistrationAttempt(ip);
        
        try {
            verifyTurnstile(request.getCfTurnstileResponse());

            // 1. Re-verify the token securely on the backend
            ProviderUserInfo userInfo = verifyProviderToken(request.getProvider(), request.getProviderToken());

            // 2. Validate uniqueness
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username is already taken.");
            }
            if (userRepository.existsByEmail(userInfo.email())) {
                throw new UserAlreadyExistsException("Email is already registered. Please sign in instead.");
            }

            // 3. Create the OAuth User
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(userInfo.email());
            user.setDob(request.getDob());
            user.setPasswordHash(null); // Explicitly null for OAuth accounts
            user.setIsVerified(true);   // Automatically verified because the provider verified it
            user.setIsBanned(false);
            user.setFalseReportStrikes(0);
            user.setShadowbannedReports(false);

            user = userRepository.save(user);

            // 4. Link the provider
            LinkedAccount linkedAccount = new LinkedAccount(user, request.getProvider(), userInfo.providerId());
            linkedAccountRepository.save(linkedAccount);

            return jwtService.generateToken(user);
            
        } catch (Exception e) {
            rateLimitService.refundRegistrationAttempt(ip);
            throw e;
        }
    }

    // ==========================================
    // HELPER: EXTERNAL TOKEN VERIFICATION
    // ==========================================
    
    private ProviderUserInfo verifyProviderToken(String provider, String token) {
        if ("GOOGLE".equalsIgnoreCase(provider)) {
            try {
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(googleClientId))
                        .build();

                GoogleIdToken idToken = verifier.verify(token);
                if (idToken != null) {
                    GoogleIdToken.Payload payload = idToken.getPayload();
                    return new ProviderUserInfo(payload.getEmail(), payload.getSubject());
                } else {
                    throw new InvalidCredentialsException("Invalid or expired Google token.");
                }
            } catch (Exception e) {
                throw new InvalidCredentialsException("Failed to verify Google token securely.");
            }
        }
        
        // When you add Discord/Steam later, just add an else-if block here!
        throw new BadRequestException("Unsupported OAuth provider: " + provider);
    }

    // A lightweight internal record to securely pass provider data around
    private record ProviderUserInfo(String email, String providerId) {}

    // ==========================================
    // HELPER: USER STATUS VALIDATION
    // ==========================================
    
    private void validateUserStatus(User user) {
        if (user.getIsVerified() != null && !user.getIsVerified()) {
            throw new AccountRestrictedException("Your account is not verified. Please check your email.");
        }

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            if (user.getBanExpiresAt() != null && user.getBanExpiresAt().isBefore(LocalDateTime.now())) {
                // Ban expired! Lift it.
                user.setIsBanned(false);
                user.setBanExpiresAt(null);
                userRepository.save(user); 
            } else {
                throw new AccountRestrictedException("Your account is currently suspended until " + 
                    (user.getBanExpiresAt() != null ? user.getBanExpiresAt().toString() : "forever."));
            }
        }
    }

    // ==========================================
    // HELPER: GET REAL IP BEHIND REVERSE PROXY
    // ==========================================
    
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

    private void verifyTurnstile(String cfResponse) {
        String clientSecretHeader = httpRequest.getHeader("X-Mobile-App-Secret");
        if (clientSecretHeader != null && clientSecretHeader.equals(mobileApiSecret)) {
            return; 
        }

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