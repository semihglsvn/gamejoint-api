package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.AccountDeleteRequest;
import com.gamejoint.gamejoint_api.dto.PasswordChangeRequest;
import com.gamejoint.gamejoint_api.dto.UserProfileResponse;
import com.gamejoint.gamejoint_api.dto.UserProfileUpdateRequest;
import com.gamejoint.gamejoint_api.exception.DuplicateResourceException;
import com.gamejoint.gamejoint_api.exception.InvalidCredentialsException;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password.pepper}")
    private String pepper;

    /**
     * Fetches the user's profile data to display on the "Settings" page.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setDob(user.getDob());
        response.setCreatedAt(user.getCreatedAt());
        
        return response;
    }

    /**
     * Updates basic profile information.
     */
    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Guard Clause: Prevent changing to a username that someone else already owns
        if (!user.getUsername().equals(request.getUsername()) && 
            userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("That username is already taken.");
        }

        user.setUsername(request.getUsername());
        user.setDob(request.getDob());
        
        // @Transactional automatically handles the UPDATE query!
    }

    /**
     * Changes the user's password securely.
     */
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Verify the current password matches MariaDB
        String pepperedCurrent = request.getCurrentPassword() + pepper;
        if (!passwordEncoder.matches(pepperedCurrent, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Incorrect current password.");
        }

        // 2. Hash and save the new password
        String pepperedNew = request.getNewPassword() + pepper;
        user.setPasswordHash(passwordEncoder.encode(pepperedNew));
    }

    /**
     * Permanently deletes the account.
     */
    @Transactional
    public void deleteAccount(Long userId, AccountDeleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Verify password before executing the deletion
        String pepperedCurrent = request.getCurrentPassword() + pepper;
        if (!passwordEncoder.matches(pepperedCurrent, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Incorrect password. Account deletion aborted.");
        }

        // 2. Delete the user
        // Note: Because of foreign keys, you may need to decide if deleting a user 
        // also deletes their reviews/reports, or if you just "anonymize" the user instead.
        userRepository.delete(user);
    }
}