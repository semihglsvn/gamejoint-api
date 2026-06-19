package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.BanRequest;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.exception.UnauthorizedOperationException;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final UserRepository userRepository;

    @Transactional
    public void banUser(Long staffId, Long targetUserId, BanRequest request) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));

        if (staff.getRole().getId() > 3) {
            throw new UnauthorizedOperationException("ACCESS DENIED: You do not have permission to ban users.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found."));

        if (targetUser.getRole().getId() <= 3) {
            throw new UnauthorizedOperationException("ACCESS DENIED: You cannot ban another staff member.");
        }

        // Apply the Ban
        targetUser.setIsBanned(true);

        // Calculate the expiration date (or leave it null for permanent)
        if (request.getDurationDays() != null && request.getDurationDays() > 0) {
            targetUser.setBanExpiresAt(LocalDateTime.now().plusDays(request.getDurationDays()));
        } else {
            targetUser.setBanExpiresAt(null); // Permanent ban
        }
        
        userRepository.save(targetUser);
    }

    @Transactional
    public void unbanUser(Long staffId, Long targetUserId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found."));

        if (staff.getRole().getId() > 3) {
            throw new UnauthorizedOperationException("ACCESS DENIED: You do not have permission to unban users.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found."));

        // Lift the Ban
        targetUser.setIsBanned(false);
        targetUser.setBanExpiresAt(null); // Clear the expiration date if they had one
        
        userRepository.save(targetUser);
    }
}