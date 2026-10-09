package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.GameScreenshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameScreenshotRepository extends JpaRepository<GameScreenshot, Long> {
    
    // Find all screenshots for a specific game ID (useful for scripts/importers)
    List<GameScreenshot> findByGameId(Long gameId);

    // Check if a game already has screenshots populated
    boolean existsByGameId(Long gameId);
}