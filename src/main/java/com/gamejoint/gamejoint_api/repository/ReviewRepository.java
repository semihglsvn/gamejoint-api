package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Keep your original method
    Page<Review> findByGameId(Long gameId, Pageable pageable);

    // --- ADD THIS NEW METHOD ---
    // Spring automatically writes the SQL to filter by Game, Role ID, and Status
    Page<Review> findByGameIdAndUserRoleIdAndStatus(
            Long gameId, 
            Long roleId, 
            Review.ReviewStatus status, 
            Pageable pageable
    );
}