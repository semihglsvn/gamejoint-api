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
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    
    // Spring Security's equivalent to PHP's password_hash() and password_verify()
    private final PasswordEncoder passwordEncoder;
    
    // A separate service we will build to handle generating the JSON Web Tokens
    private final JwtService jwtService;
    
    // Spring's built-in HTTP client for making external API calls (to Cloudflare)
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cloudflare.turnstile.secret}")
    private String turnstileSecret;

    /**
     * Handles New User Registration
     */
    @Transactional
    public void register(UserRegistrationRequest request) {
        
        // 1. Verify they are human first before touching the database
        verifyTurnstile(request.getCfTurnstileResponse());

        // 2. Prevent Duplicate Accounts
        if (userRepository.existsByUsername(request.getUsername()) ||
            userRepository.existsByEmail(request.getEmail())) {
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

        // 4. Save to MariaDB
        userRepository.save(user);

        // TODO: Call AccountRecoveryService to send the verification email here!
    }

    /**
     * Handles User Login and Token Generation
     */
    public String login(UserLoginRequest request) {
        
        verifyTurnstile(request.getCfTurnstileResponse());

        // We need this custom method in UserRepository: 
        // Optional<User> findByUsernameOrEmail(String username, String email);
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("No account found with that username or email."));

        // PHP equivalent: if (!password_verify($password, $user['password_hash']))
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password.");
        }

        // --- THE BOUNCER CHECKS ---
        if (user.getIsVerified() != null && !user.getIsVerified()) {
            throw new AccountRestrictedException("Your account is not verified. Please check your email.");
        }

        if (user.getIsBanned() != null && user.getIsBanned()) {
            throw new AccountRestrictedException("Your account has been banned.");
        }

        // If they survive the bouncer, generate and return their JWT token
        return jwtService.generateToken(user);
    }

    /**
     * Validates the Cloudflare Turnstile Captcha
     */
    private void verifyTurnstile(String cfResponse) {
        if (cfResponse == null || cfResponse.isBlank()) {
            throw new InvalidCredentialsException("Security widget failed to load. Please try again.");
        }

        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        
        // Build the payload
        var request = Map.of(
            "secret", turnstileSecret,
            "response", cfResponse
        );
     // Send the POST request to Cloudflare and grab the body directly
        @SuppressWarnings("unchecked")
        Map<String, Object> body = restTemplate.postForObject(url, request, Map.class);
        
        if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
            throw new InvalidCredentialsException("Cloudflare verification failed. Are you a bot?");
        }
        }
    }
