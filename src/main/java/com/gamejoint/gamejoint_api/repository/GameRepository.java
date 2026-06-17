package com.gamejoint.gamejoint_api.repository;
import com.gamejoint.gamejoint_api.model.Game;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
public interface GameRepository extends JpaRepository<Game, Long> {

	Page<Game> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
	
	// Spring writes: SELECT * FROM games WHERE metascore >= ?
	Page<Game> findByMetascoreGreaterThanEqual(Integer score, Pageable pageable);

	// Spring writes: SELECT * FROM games WHERE release_date >= ?
	Page<Game> findByReleaseDateAfter(LocalDate date, Pageable pageable);
}
