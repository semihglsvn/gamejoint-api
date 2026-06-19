package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.ReviewCreateRequest;
import com.gamejoint.gamejoint_api.dto.ReviewUpdateRequest;
import com.gamejoint.gamejoint_api.exception.AccountRestrictedException;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.exception.UnauthorizedOperationException;
import com.gamejoint.gamejoint_api.model.Game;
import com.gamejoint.gamejoint_api.model.Review;
import com.gamejoint.gamejoint_api.model.User;
import com.gamejoint.gamejoint_api.repository.GameRepository;
import com.gamejoint.gamejoint_api.repository.ReviewRepository;
import com.gamejoint.gamejoint_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    @Transactional
    public void createReview(Long userId, ReviewCreateRequest request) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.getIsVerified()) {
            throw new RuntimeException("You must verify your email address before posting a review.");
        }
        // Guard Clause: Banned users cannot post reviews
        if (user.getIsBanned() != null && user.getIsBanned()) {
            throw new AccountRestrictedException("Your account is restricted. You cannot post reviews.");
        }
        
     // 2. The Staff Block
        // Roles 1 (Admin), 2 (Editor), 3 (Moderator)
        if (user.getRole().getId() <= 3) {
            throw new RuntimeException("Staff members are not permitted to post game reviews.");
        }

        // 3. The Critic vs. User Scale Validation
        if (user.getRole().getId() == 4) {
            // Critic Logic: Out of 100
            if (request.getScore() < 1 || request.getScore() > 100) {
                throw new IllegalArgumentException("Critics must provide a rating between 1 and 100.");
            }
        } else {
            // Standard User Logic: Out of 10
            if (request.getScore() < 1 || request.getScore() > 10) {
                throw new IllegalArgumentException("Users must provide a rating between 1 and 10.");
            }
        }

        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        // Optional best practice: Ensure one review per user, per game
        // (You would need to add existsByUserIdAndGameId to your ReviewRepository first)
        // if (reviewRepository.existsByUserIdAndGameId(userId, game.getId())) {
        //     throw new DuplicateResourceException("You have already reviewed this game.");
        // }

        // The Happy Path
        Review review = new Review();
        review.setUser(user);
        review.setGame(game);
        review.setScore(request.getScore());
        review.setComment(request.getComment());
        
        // Status defaults to ReviewStatus.APPROVED instantly because we hardcoded 
        // it in your Review.java entity class earlier.
        
        reviewRepository.save(review);
    }


    /**
     * Updates an existing review, proving ownership first.
     */
    @Transactional
    public void updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        
        // 1. Fetch the existing review
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // 2. Fetch the user to check their role for the scale validation
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. The Ownership Guard Clause (Crucial!)
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("You can only edit your own reviews.");
        }

        // 4. The Critic vs. User Scale Validation (Protect against illegal updates)
        if (user.getRole().getId() == 4) {
            if (request.getScore() < 1 || request.getScore() > 100) {
                throw new IllegalArgumentException("Critics must provide a rating between 1 and 100.");
            }
        } else {
            if (request.getScore() < 1 || request.getScore() > 10) {
                throw new IllegalArgumentException("Users must provide a rating between 1 and 10.");
            }
        }

        // 5. The Happy Path (Apply changes)
        review.setScore(request.getScore());
        review.setComment(request.getComment());
        
        // Because of the @Transactional annotation, Hibernate automatically detects 
        // that the 'review' object changed and writes the UPDATE SQL to MariaDB.
    }
/**
 * Deletes a review, proving ownership first.
 */
@Transactional
public void deleteReview(Long userId, Long reviewId) {
    
    Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

    // The Ownership Guard Clause
    if (!review.getUser().getId().equals(userId)) {
        throw new UnauthorizedOperationException("You can only delete your own reviews.");
    }

    // Optional: If you want admins to be able to delete any review, 
    // you would check the user's role here before throwing the exception.

    reviewRepository.delete(review);
}
}