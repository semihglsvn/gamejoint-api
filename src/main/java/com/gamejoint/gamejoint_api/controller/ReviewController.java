package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.ReviewCreateRequest;
import com.gamejoint.gamejoint_api.dto.ReviewUpdateRequest;
import com.gamejoint.gamejoint_api.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Endpoint: POST /api/reviews
     * Allows a logged-in user to post a new review.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createReview(
            @RequestAttribute("userId") Long userId,
            @RequestBody ReviewCreateRequest request) {
        
        reviewService.createReview(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Review posted successfully."));
    }

    /**
     * Endpoint: PUT /api/reviews/42
     * Allows a user to edit their existing review.
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> updateReview(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request) {
        
        // The service layer handles the "Ownership Guard Clause" to ensure 
        // the user actually owns review #42 before updating it.
        reviewService.updateReview(userId, reviewId, request);
        
        return ResponseEntity.ok(Map.of("message", "Review updated successfully."));
    }

    /**
     * Endpoint: DELETE /api/reviews/42
     * Allows a user to delete their own review.
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long reviewId) {
        
        reviewService.deleteReview(userId, reviewId);
        
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully."));
    }
}