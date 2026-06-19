package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.BanRequest;
import com.gamejoint.gamejoint_api.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    // POST http://localhost:8080/api/moderation/users/55/ban
    @PostMapping("/users/{targetUserId}/ban")
    public ResponseEntity<String> banUser(
            @RequestAttribute("userId") Long staffId, 
            @PathVariable Long targetUserId,
            @RequestBody BanRequest request) { // <--- Added the body here

        moderationService.banUser(staffId, targetUserId, request);
        
        String message = request.getDurationDays() != null 
                ? "User suspended for " + request.getDurationDays() + " days." 
                : "User permanently banned.";
                
        return ResponseEntity.ok(message);
    }
    // POST http://localhost:8080/api/moderation/users/55/unban
    @PostMapping("/users/{targetUserId}/unban")
    public ResponseEntity<String> unbanUser(
            @RequestAttribute("userId") Long staffId,
            @PathVariable Long targetUserId) {

        moderationService.unbanUser(staffId, targetUserId);
        
        return ResponseEntity.ok("User has been successfully unbanned.");
    }
}