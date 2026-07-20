package com.gamejoint.gamejoint_api.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.gamejoint.gamejoint_api.dto.GameDetail;
import com.gamejoint.gamejoint_api.dto.GameSummary;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.Game;
import com.gamejoint.gamejoint_api.repository.GameRepository;
import com.gamejoint.gamejoint_api.specification.GameSpecification;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

	private final GameRepository gameRepository;
	private final com.gamejoint.gamejoint_api.repository.FeaturedGameRepository featuredGameRepository;

	@Transactional(readOnly = true)
	public Page<GameSummary> getAllGames(Pageable pageable) {
		Page<Game> games = gameRepository.findAll(pageable);
		return games.map(this::mapToSummary);
	}

	@Transactional(readOnly = true)
	public Page<GameSummary> searchGames(String keyword, Pageable pageable) {
		Page<Game> games = gameRepository.findByTitleContainingIgnoreCase(keyword, pageable);
		return games.map(this::mapToSummary);
	}

	@Transactional(readOnly = true)
	public GameDetail getGameById(Long id) {
		Game game = gameRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Game not found with ID: " + id));
		return mapToDetail(game);

	}
	@Transactional(readOnly = true)
    @Cacheable("topRatedGames")
    public Page<GameSummary> getTopRatedGames(Pageable pageable) {
        Page<Game> games = gameRepository.findByMetascoreGreaterThanEqual(85, pageable);
        return games.map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    @Cacheable("newReleases")
    public Page<GameSummary> getNewReleases(Pageable pageable) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        Page<Game> games = gameRepository.findByReleaseDateAfter(thirtyDaysAgo, pageable);
        return games.map(this::mapToSummary);
    }
    @Transactional(readOnly = true)
    public java.util.List<com.gamejoint.gamejoint_api.dto.FeaturedGameResponse> getFeaturedGames() {
        return featuredGameRepository.findAllByOrderByDisplayOrderAsc().stream()
            .map(fg -> {
                com.gamejoint.gamejoint_api.dto.FeaturedGameResponse dto = new com.gamejoint.gamejoint_api.dto.FeaturedGameResponse();
                
                // Basic Info
                dto.setGameId(fg.getGame().getId());
                dto.setTitle(fg.getGame().getTitle());
                dto.setCustomBanner(fg.getCustomBanner());
                dto.setCoverImage(fg.getGame().getCoverImage());
                
                // Add Metascore
                dto.setMetascore(fg.getGame().getMetascore());
                
                // Add Genres
                if (fg.getGame().getGenres() != null) {
                    dto.setGenres(fg.getGame().getGenres().stream()
                        .map(genre -> genre.getName())
                        .collect(Collectors.toList())); // Change to .toSet() if your DTO uses Set<String>
                }
                
                // Add Platforms
                if (fg.getGame().getPlatforms() != null) {
                    dto.setPlatforms(fg.getGame().getPlatforms().stream()
                        .map(platform -> platform.getName())
                        .collect(Collectors.toList())); // Change to .toSet() if your DTO uses Set<String>
                }
                
                return dto;
            }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable("trendingGames")
    public Page<GameSummary> getTrendingGames(Pageable pageable) {
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        
        // 1. Get whatever trending games we have
        Page<Game> trendingGames = gameRepository.findTrendingGames(thirtyDaysAgo, pageable);
        List<Game> finalList = new ArrayList<>(trendingGames.getContent());
        
        // 2. If we have less than the requested size (e.g., 15), pad with Top Rated
        if (finalList.size() < pageable.getPageSize()) {
            int amountNeeded = pageable.getPageSize() - finalList.size();
            List<Long> existingIds = finalList.stream().map(Game::getId).collect(Collectors.toList());
            
            // Fetch Top Rated games to fill the gap
         // We add Sort.by(Sort.Direction.DESC, "metascore") to force the absolute highest rated games to the top
            Page<Game> paddingGames = gameRepository.findByMetascoreGreaterThanEqual(
                85, 
                PageRequest.of(0, amountNeeded + 5, Sort.by(Sort.Direction.DESC, "metascore"))
            );            
            for (Game g : paddingGames) {
                if (!existingIds.contains(g.getId())) {
                    finalList.add(g);
                }
                if (finalList.size() >= pageable.getPageSize()) break;
            }
        }
        
        // Return as a proper Page object
        return new PageImpl<>(finalList, pageable, finalList.size()).map(this::mapToSummary);
    }
	// ==========================================
	// PRIVATE MAPPING HELPERS
	// ==========================================

    private GameSummary mapToSummary(Game game) {
        GameSummary dto = new GameSummary();
        dto.setId(game.getId());
        dto.setTitle(game.getTitle());
        dto.setMetascore(game.getMetascore());
        dto.setReleaseDate(game.getReleaseDate());
        
        // Use our new optimizer method for the Summary DTO!
        dto.setCoverImage(optimizeImageUrl(game.getCoverImage()));
        
        // (Map your genres/platforms here as usual)
        return dto;
    }

    /**
     * Helper method to convert full HD RAWG images into lightweight thumbnails.
     * Example: converts "media.rawg.io/media/games/ca1/..." 
     * to "media.rawg.io/media/resize/640/-/games/ca1/..."
     */
    private String optimizeImageUrl(String originalUrl) {
        if (originalUrl != null && originalUrl.contains("media.rawg.io/media/")) {
            return originalUrl.replace(
                "media.rawg.io/media/", 
                "media.rawg.io/media/resize/640/-/"
            );
        }
        return originalUrl;
    }
	@Transactional(readOnly = true)
    public Page<GameSummary> searchGames(
            String query, 
            Integer minMetascore, 
            Boolean hideTbd, 
            List<String> genres, 
            List<String> platforms, 
            Boolean isMatchAll, // Add this!
            Pageable pageable
    ) {
        Specification<Game> spec = GameSpecification.withFilters(query, minMetascore, hideTbd, genres, platforms, isMatchAll);
        Page<Game> games = gameRepository.findAll(spec, pageable);
        return games.map(this::mapToSummary);
    }

	private GameDetail mapToDetail(Game game) {
		GameDetail dto = new GameDetail();
		dto.setId(game.getId());
		dto.setTitle(game.getTitle());
		dto.setDescription(game.getDescription());
		dto.setDeveloper(game.getDeveloper());
		dto.setPublisher(game.getPublisher());
		dto.setReleaseDate(game.getReleaseDate());
		dto.setEsrbRating(game.getEsrbRating());
		dto.setMetascore(game.getMetascore());
		dto.setCoverImage(game.getCoverImage());

		if (game.getPlatforms() != null) {
			dto.setPlatformNames(
					game.getPlatforms().stream().map(platform -> platform.getName()).collect(Collectors.toSet()));
		}

		if (game.getGenres() != null) {
			dto.setGenreNames(game.getGenres().stream().map(genre -> genre.getName()).collect(Collectors.toSet()));
		}

		return dto;
	}
}