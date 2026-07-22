package com.gamejoint.gamejoint_api.repository;
import com.gamejoint.gamejoint_api.model.Game;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
public interface GameRepository extends JpaRepository<Game, Long> , JpaSpecificationExecutor<Game> {

	Page<Game> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
	
	// Spring writes: SELECT * FROM games WHERE metascore >= ?
	Page<Game> findByMetascoreGreaterThanEqual(Integer score, Pageable pageable);

	// Spring writes: SELECT * FROM games WHERE release_date >= ?
	Page<Game> findByReleaseDateAfter(LocalDate date, Pageable pageable);
	// Add this inside GameRepository.java
	Page<Game> findByReleaseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
	@Query("SELECT g FROM Game g JOIN Review r ON r.game = g WHERE r.createdAt >= :since GROUP BY g.id ORDER BY COUNT(r.id) DESC")
    Page<Game> findTrendingGames(@Param("since") LocalDateTime since, Pageable pageable);
}
