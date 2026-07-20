package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.ReviewCreateRequest;
import com.gamejoint.gamejoint_api.dto.ReviewResponse;
import com.gamejoint.gamejoint_api.dto.ReviewUpdateRequest;
import com.gamejoint.gamejoint_api.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
     * Endpoint: GET /api/reviews/game/{gameId}?roleId=4&page=0&size=10
     * Fetches approved reviews for a game. Defaults to standard users (roleId=5).
     */
    @GetMapping("/game/{gameId}")
    public ResponseEntity<Page<ReviewResponse>> getGameReviews(
            @PathVariable Long gameId,
            @RequestParam(defaultValue = "5") Long roleId, // 4 = Critic, 5 = User
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // Sort by newest reviews first
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return ResponseEntity.ok(reviewService.getReviewsForGame(gameId, roleId, pageable));
    }

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
     * Endpoint: PUT /api/reviews/{reviewId}
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
     * Endpoint: DELETE /api/reviews/{reviewId}
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