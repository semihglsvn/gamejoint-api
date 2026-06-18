package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.AccountDeleteRequest;
import com.gamejoint.gamejoint_api.dto.PasswordChangeRequest;
import com.gamejoint.gamejoint_api.dto.UserProfileResponse;
import com.gamejoint.gamejoint_api.dto.UserProfileUpdateRequest;
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

    /**
     * Endpoint: GET /api/users/profile
     * Fetches the user's data to display on the "Settings" screen.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestAttribute("userId") Long userId) {
        
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    /**
     * Endpoint: PUT /api/users/profile
     * Allows the user to update their username and date of birth.
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(
            @RequestAttribute("userId") Long userId,
            @RequestBody UserProfileUpdateRequest request) {
        
        userService.updateProfile(userId, request);
        
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }

    /**
     * Endpoint: PUT /api/users/password
     * Allows a logged-in user to change their password securely.
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestAttribute("userId") Long userId,
            @RequestBody PasswordChangeRequest request) {
        
        userService.changePassword(userId, request);
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    /**
     * Endpoint: DELETE /api/users/account
     * Permanently deletes the user's account (requires current password validation).
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestAttribute("userId") Long userId,
            @RequestBody AccountDeleteRequest request) {
        
        userService.deleteAccount(userId, request);
        
        return ResponseEntity.ok(Map.of("message", "Account successfully deleted."));
    }
}