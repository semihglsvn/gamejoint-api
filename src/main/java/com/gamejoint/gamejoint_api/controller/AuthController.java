package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.*;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.service.AccountRecoveryService;
import com.gamejoint.gamejoint_api.service.AuthService;
import com.gamejoint.gamejoint_api.service.TurnstileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService recoveryService;
    private final TurnstileService turnstileService;

    @Value("${mobile.api.secret}")
    private String mobileApiSecret;

    // ==========================================
    // CORE AUTHENTICATION
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody @Valid UserRegistrationRequest request,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {

        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        authService.register(request);        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid UserLoginRequest request,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {

        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        String jwtToken = authService.login(request);
        ResponseCookie cookie = generateJwtCookie(jwtToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(jwtToken));
    }

    // ==========================================
    // OAUTH AUTHENTICATION
    // ==========================================

    @PostMapping("/oauth/login")
    public ResponseEntity<?> oauthLogin(
            @RequestBody @Valid OAuthLoginRequest request,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {

        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        OAuthAuthResponse response = authService.oauthLogin(request);

        // If the user is new, we do NOT issue a JWT yet. We return 202 Accepted to prompt profile completion.
        if (response.isNewUser()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }

        // If the user exists or was auto-linked, issue the cookie and log them in immediately.
        ResponseCookie cookie = generateJwtCookie(response.getJwtToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/oauth/complete")
    public ResponseEntity<?> completeOAuthRegistration(
            @RequestBody @Valid OAuthRegistrationCompleteRequest request,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {

        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        String jwtToken = authService.completeOAuthRegistration(request);
        ResponseCookie cookie = generateJwtCookie(jwtToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(jwtToken));
    }

    // ==========================================
    // LOGOUT
    // ==========================================

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged out successfully."));
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private boolean isMobileRequest(String headerSecret) {
        return headerSecret != null && headerSecret.equals(mobileApiSecret);
    }

    private ResponseCookie generateJwtCookie(String jwtToken) {
        return ResponseCookie.from("jwt", jwtToken)
                .httpOnly(true)
                .secure(true) 
                .path("/")
                .maxAge(365 * 24 * 60 * 60) 
                .sameSite("Lax")
                .build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If the HttpOnly cookie is valid, the filter will have populated this context
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", "USER" // Adjust this if you have a dynamic role mapping!
            ));
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
 // ==========================================
    // ACCOUNT RECOVERY & VERIFICATION (OTP ONLY)
    // ==========================================

    @PostMapping("/verify/resend")
    public ResponseEntity<Map<String, String>> resendVerification(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {
        
        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        String identifier = body.get("identifier"); 
        recoveryService.resendVerificationEmail(identifier);
        return ResponseEntity.ok(Map.of("message", "If that account exists and is unverified, a new code has been sent."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyAccount(
            @RequestBody OtpVerifyRequest request,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {
            
        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        recoveryService.verifyAccount(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(Map.of("message", "Account successfully verified."));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {
            
        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        String email = body.get("email");
        recoveryService.requestPasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset code has been sent."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody OtpPasswordResetRequest request,
            @RequestHeader(value = "X-Mobile-App-Secret", required = false) String mobileSecretHeader,
            @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken) {
            
        if (!isMobileRequest(mobileSecretHeader) && !turnstileService.verifyToken(turnstileToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "CAPTCHA validation failed. Please try again."));
        }

        recoveryService.executePasswordReset(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now log in."));
    }
    }
