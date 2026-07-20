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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AccountRecoveryService accountRecoveryService;
    
    // Inject the HttpServletRequest to check for headers
    private final HttpServletRequest httpRequest;

    @Value("${cloudflare.turnstile.secret}")
    private String turnstileSecret;

    @Value("${mobile.api.secret}")
    private String mobileApiSecret; // Add this to your application.properties!

    @Transactional
    public void register(UserRegistrationRequest request) {

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

        accountRecoveryService.resendVerificationEmail(user.getEmail()); // The call is now here!
    }

    public String login(UserLoginRequest request) {

        verifyTurnstile(request.getCfTurnstileResponse());

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No account found with that username or email."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password.");
        }

        if (user.getIsVerified() != null && !user.getIsVerified()) {
            throw new AccountRestrictedException("Your account is not verified. Please check your email.");
        }

        if (user.getIsBanned() != null && user.getIsBanned()) {
            if (user.getBanExpiresAt() != null && java.time.LocalDateTime.now().isAfter(user.getBanExpiresAt())) {
                user.setIsBanned(false);
                user.setBanExpiresAt(null);
                userRepository.save(user);
            } else {
                if (user.getBanExpiresAt() != null) {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                    String expireDate = user.getBanExpiresAt().format(formatter);
                    throw new AccountRestrictedException("Your account is suspended until " + expireDate + ".");
                } else {
                    throw new AccountRestrictedException("Your account has been permanently banned.");
                }
            }
        }

        return jwtService.generateToken(user);
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