package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.dto.GameDetail;
import com.gamejoint.gamejoint_api.dto.GameSummary;
import com.gamejoint.gamejoint_api.exception.ResourceNotFoundException;
import com.gamejoint.gamejoint_api.model.Game;
import com.gamejoint.gamejoint_api.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

	private final GameRepository gameRepository;

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
    public Page<GameSummary> getTopRatedGames(Pageable pageable) {
        // Only fetch games with a Metascore of 85 or higher
        Page<Game> games = gameRepository.findByMetascoreGreaterThanEqual(85, pageable);
        return games.map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public Page<GameSummary> getNewReleases(Pageable pageable) {
        // Calculate the date 30 days ago
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        // Only fetch games released in the last 30 days
        Page<Game> games = gameRepository.findByReleaseDateAfter(thirtyDaysAgo, pageable);
        return games.map(this::mapToSummary);
    }

	// ==========================================
	// PRIVATE MAPPING HELPERS
	// ==========================================

	private GameSummary mapToSummary(Game game) {
		GameSummary dto = new GameSummary();
		dto.setId(game.getId());
		dto.setTitle(game.getTitle());
		dto.setCoverImage(game.getCoverImage());
		dto.setMetascore(game.getMetascore());
		dto.setReleaseDate(game.getReleaseDate());

		if (game.getPlatforms() != null) {
			dto.setPlatforms(
					game.getPlatforms().stream().map(platform -> platform.getName()).collect(Collectors.toSet()));
		}

		if (game.getGenres() != null) {
			dto.setGenres(game.getGenres().stream().map(genre -> genre.getName()).collect(Collectors.toSet()));
		}

		return dto;
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