package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Spring writes the SQL: SELECT * FROM reviews WHERE game_id = ? 
    // We use Pageable because popular games might have thousands of reviews!
    Page<Review> findByGameId(Long gameId, Pageable pageable);

}