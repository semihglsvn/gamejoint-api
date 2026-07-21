package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.OtpPasswordResetRequest;
import com.gamejoint.gamejoint_api.dto.OtpVerifyRequest;
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserLoginRequest request) {
        String jwtToken = authService.login(request);
        return ResponseEntity.ok(new TokenResponse(jwtToken));
    }

    // ==========================================
    // WEB ENDPOINTS (Magic Links)
    // ==========================================

    @PostMapping("/verify/resend")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier"); 
        recoveryService.resendVerificationEmail(identifier);
        return ResponseEntity.ok(Map.of("message", "If that account exists and is unverified, a new link has been sent."));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        recoveryService.requestPasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody PasswordResetExecuteRequest request) {
        recoveryService.executePasswordReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now log in."));
    }

    // ==========================================
    // MOBILE ENDPOINTS (6-Digit OTP)
    // ==========================================

    @PostMapping("/verify/otp")
    public ResponseEntity<Map<String, String>> verifyAccountOtp(@RequestBody OtpVerifyRequest request) {
        
        recoveryService.verifyAccountOtp(request.getIdentifier(), request.getOtp());
        
        return ResponseEntity.ok(Map.of("message", "Account successfully verified."));
    }

    @PostMapping("/password/reset/otp")
    public ResponseEntity<Map<String, String>> resetPasswordOtp(@RequestBody OtpPasswordResetRequest request) {
        
        recoveryService.executePasswordResetOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
        
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now log in."));
    }
}