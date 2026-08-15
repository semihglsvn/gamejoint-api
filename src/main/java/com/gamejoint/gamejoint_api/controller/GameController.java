package com.gamejoint.gamejoint_api.controller;

import com.gamejoint.gamejoint_api.dto.GameDetail;
import com.gamejoint.gamejoint_api.dto.GameSummary;
import com.gamejoint.gamejoint_api.service.GameService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
     * Fetches the main catalog, sorted by newest releases first.
     */
    @GetMapping
    public ResponseEntity<Page<GameSummary>> getAllGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "releaseDate"));
        return ResponseEntity.ok(gameService.getAllGames(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<GameSummary>> searchGames(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minMetascore,
            @RequestParam(required = false) Boolean hideTbd,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) List<String> platforms,
            @RequestParam(required = false, defaultValue = "false") Boolean isMatchAll, // NEW!
            @RequestParam(required = false, defaultValue = "Highest Rated") String sortBy, // NEW!
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Determine the Sorting Logic
        Sort sort;
        switch (sortBy) {
            case "Lowest Rated":
                sort = Sort.by(Sort.Direction.ASC, "metascore");
                break;
            case "Newest First":
                sort = Sort.by(Sort.Direction.DESC, "releaseDate");
                break;
            case "Oldest First":
                sort = Sort.by(Sort.Direction.ASC, "releaseDate");
                break;
            case "Highest Rated":
            default:
                sort = Sort.by(Sort.Direction.DESC, "metascore");
                break;
        }

        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<GameSummary> results = gameService.searchGames(q, minMetascore, hideTbd, genres, platforms, isMatchAll, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Endpoint: GET /api/games/top-rated
     * Feeds the "Top Rated" horizontal slider on the UI.
     */
    @GetMapping("/top-rated")
    public ResponseEntity<Page<GameSummary>> getTopRatedGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "metascore"));
        return ResponseEntity.ok(gameService.getTopRatedGames(pageable));
    }

    /**
     * Endpoint: GET /api/games/new-releases
     * Feeds the "New Releases" horizontal slider on the UI.
     */
    @GetMapping("/new-releases")
    public ResponseEntity<Page<GameSummary>> getNewReleases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "releaseDate"));
        return ResponseEntity.ok(gameService.getNewReleases(pageable));
    }
    /**
     * Endpoint: GET /api/games/featured
     * Feeds the large hero carousel at the top of the Home Screen.
     */
    @GetMapping("/featured")
    public ResponseEntity<java.util.List<com.gamejoint.gamejoint_api.dto.FeaturedGameResponse>> getFeaturedGames() {
        return ResponseEntity.ok(gameService.getFeaturedGames());
    }

    /**
     * Endpoint: GET /api/games/trending
     * Feeds the "Trending" horizontal slider (most reviews in last 30 days).
     */
    @GetMapping("/trending")
    public ResponseEntity<Page<GameSummary>> getTrendingGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(gameService.getTrendingGames(pageable));
    }
    /**
     * Endpoint: GET /api/games/count
     * Returns the total row count of games for sitemap chunk calculation.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getGamesCount() {
        return ResponseEntity.ok(gameService.getGamesCount());
    }

    /**
     * Endpoint: GET /api/games/sitemap?page=0&size=50000
     * Returns a lightweight projection of ID and UpdatedAt for indexing.
     */
    @GetMapping("/sitemap")
    public ResponseEntity<List<com.gamejoint.gamejoint_api.dto.GameSitemap>> getGamesForSitemap(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50000") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return ResponseEntity.ok(gameService.getGamesForSitemap(pageable));
    }
    /**
     * Endpoint: GET /api/games/452
     * Fetches the heavy details for a single game's dedicated page.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameDetail> getGameById(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGameById(id));
        
        
        
        
    }
}