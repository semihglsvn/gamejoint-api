package com.gamejoint.gamejoint_api.repository;
import com.gamejoint.gamejoint_api.model.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
public interface GameRepository extends JpaRepository<Game, Long> {

	Page<Game> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
