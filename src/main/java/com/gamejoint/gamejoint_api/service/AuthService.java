package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.UserLoginRequest;
import com.gamejoint.gamejoint_api.dto.UserRegistrationRequest;
import com.gamejoint.gamejoint_api.exception.AccountRestrictedException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.UserAlreadyExistsException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
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

    @Value("${cloudflare.turnstile.secret}")
    private String turnstileSecret;

    @Transactional
    public void register(UserRegistrationRequest request) {

        // 1. Verify they are human first before touching the database
        verifyTurnstile(request.getCfTurnstileResponse());

        // 2. Prevent Duplicate Accounts
        if (userRepository.existsByUsername(request.getUsername())
                || userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Username or Email is already taken.");
        }

        // 3. Create the Entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDob(request.getDob());

        // Safely hash the password before saving
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Defaults
        user.setIsVerified(false);
        user.setIsBanned(false);
        user.setFalseReportStrikes(0);
        user.setShadowbannedReports(false);

        // 4. Save to Database
        userRepository.save(user);

        // TODO: Call AccountRecoveryService to send the verification email here!
    }

    public String login(UserLoginRequest request) {

        // Verify human interaction
        verifyTurnstile(request.getCfTurnstileResponse());

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No account found with that username or email."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password.");
        }

        // --- THE BOUNCER CHECKS ---
        if (user.getIsVerified() != null && !user.getIsVerified()) {
            throw new AccountRestrictedException("Your account is not verified. Please check your email.");
        }

        // --- THE LAZY UNBAN AND BANNED CHECK ---
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