package com.gameStore.Bino.service;

import com.gameStore.Bino.exceptions.ResourceNotFoundException;
import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.repositories.GamesRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class GamesService {
    private final GamesRepository gamesRepository;

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    /**
     * Paginated list with optional keyword + genre filters. Replaces three
     * previously separate methods (findAllGames / searchGames / getGamesByGenre)
     * that all did roughly the same thing — a single repository query now covers
     * every /games/all shape the frontend needs.
     *
     * Pageable carries page/size/sort from the request; the controller supplies
     * a default (id ASC) so pagination is deterministic when the caller omits
     * a sort.
     */
    public Page<Games> search(String q, String genre, Pageable pageable) {
        return gamesRepository.search(q, genre, pageable);
    }

    public Games getGameById(Long id) {
        return gamesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + id));
    }

    // ---------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------

    public Games addGame(Games game) {
        return gamesRepository.save(game);
    }

    @Transactional
    public void deleteGames(Long id) {
        if (!gamesRepository.existsById(id)) {
            throw new ResourceNotFoundException("Game not found with id: " + id);
        }
        gamesRepository.deleteById(id);
    }

    public Games updateGame(Long id, Games updatedGame) {
        Games existing = getGameById(id);  // throws if not found

        existing.setTitle(updatedGame.getTitle());
        existing.setGenre(updatedGame.getGenre());
        existing.setDescription(updatedGame.getDescription());
        existing.setPrice(updatedGame.getPrice());
        existing.setRating(updatedGame.getRating());
        existing.setImageUrl(updatedGame.getImageUrl());
        existing.setHeroImage(updatedGame.getHeroImage());

        return gamesRepository.save(existing);
    }
}
