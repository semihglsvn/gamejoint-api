package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByGameId(Long gameId, Pageable pageable);

    Page<Review> findByGameIdAndUserRoleIdAndStatus(
            Long gameId, 
            Long roleId, 
            Review.ReviewStatus status, 
            Pageable pageable
    );

    // CHANGED: Now searches by the User's Username!
    List<Review> findByUserUsernameAndStatusOrderByCreatedAtDesc(String username, Review.ReviewStatus status);
}