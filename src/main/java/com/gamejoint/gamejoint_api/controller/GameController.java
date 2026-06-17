package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.GameDetail;
import com.gamejoint.gamejoint_api.dto.GameSummary;
import com.gamejoint.gamejoint_api.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * Endpoint: GET /api/games?page=0&size=20
     * Fetches the catalog of games.
     */
    @GetMapping
    public ResponseEntity<Page<GameSummary>> getAllGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Best practice: Automatically sort the catalog so the newest releases show up first
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "releaseDate"));
        
        return ResponseEntity.ok(gameService.getAllGames(pageable));
    }

    /**
     * Endpoint: GET /api/games/search?q=witcher&page=0&size=20
     * Allows the mobile app to search games.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<GameSummary>> searchGames(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(gameService.searchGames(query, pageable));
    }

    /**
     * Endpoint: GET /api/games/452
     * Fetches the heavy details for a single game page.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameDetail> getGameById(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGameById(id));
    }
}