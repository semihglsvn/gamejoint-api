package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.PasswordResetExecuteRequest;
import com.gamejoint.gamejoint_api.dto.TokenResponse;
import com.gamejoint.gamejoint_api.dto.UserLoginRequest;
import com.gamejoint.gamejoint_api.dto.UserRegistrationRequest;
import com.gamejoint.gamejoint_api.service.AccountRecoveryService;
import com.gamejoint.gamejoint_api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService recoveryService;

    // ==========================================
    // CORE AUTHENTICATION
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody UserRegistrationRequest request) {
        authService.register(request);
        
        // Immediately trigger the verification email after successful registration
        recoveryService.resendVerificationEmail(request.getEmail());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request) {
        // This will throw our custom exceptions if banned, unverified, or wrong password
        String jwtToken = authService.login(request);
        
        // Return the VIP pass as a clean JSON object
        return ResponseEntity.ok(new TokenResponse(jwtToken));
    }

    // ==========================================
    // ACCOUNT RECOVERY (PUBLIC ENDPOINTS)
    // ==========================================

    @PostMapping("/verify/resend")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier"); // Can be username or email
        recoveryService.resendVerificationEmail(identifier);
        
        return ResponseEntity.ok(Map.of("message", "If that account exists and is unverified, a new link has been sent."));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        recoveryService.requestPasswordReset(email);
        
        // Anti-hacker generic response
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody PasswordResetExecuteRequest request) {
        recoveryService.executePasswordReset(request.getToken(), request.getNewPassword());
        
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now log in."));
    }
}