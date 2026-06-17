package com.gamejoint.gamejoint_api.repository;

import com.gamejoint.gamejoint_api.model.FeaturedGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeaturedGameRepository extends JpaRepository<FeaturedGame, Long> {

    // Spring writes the SQL: SELECT * FROM featured_games ORDER BY display_order ASC
    // We use List instead of Page because we know there are only 16 of them.
    List<FeaturedGame> findAllByOrderByDisplayOrderAsc();

}