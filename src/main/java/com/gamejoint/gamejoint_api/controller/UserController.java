package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.AccountDeleteRequest;
import com.gamejoint.gamejoint_api.dto.EmailChangeRequest;
import com.gamejoint.gamejoint_api.dto.PasswordChangeRequest;
import com.gamejoint.gamejoint_api.dto.UserProfileResponse;
import com.gamejoint.gamejoint_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    /**
     * Endpoint: POST /api/users/settings/otp
     * Generates a 6-digit code and emails it to the currently logged-in user.
     */
    @PostMapping("/settings/otp")
    public ResponseEntity<Map<String, String>> requestSettingsOtp(@RequestAttribute("userId") Long userId) {
        userService.requestSettingsOtp(userId);
        return ResponseEntity.ok(Map.of("message", "A verification code has been sent to your email."));
    }

    @PutMapping("/email")
    public ResponseEntity<Map<String, String>> changeEmail(
            @RequestAttribute("userId") Long userId,
            @RequestBody EmailChangeRequest request) {
        userService.changeEmail(userId, request);
        return ResponseEntity.ok(Map.of("message", "Email updated successfully. You have been logged out."));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestAttribute("userId") Long userId,
            @RequestBody PasswordChangeRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. You have been logged out."));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestAttribute("userId") Long userId,
            @RequestBody AccountDeleteRequest request) {
        userService.deleteAccount(userId, request);
        return ResponseEntity.ok(Map.of("message", "Account successfully deleted."));
    }
}