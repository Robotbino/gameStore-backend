package com.gameStore.Bino.service;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.repositories.GamesRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class GamesService {
    private final GamesRepository gamesRepository;

    //Read-Operations
    //Show all games
    public List<Games> findAllGames()
    {
        return gamesRepository.findAll();
    }

    //Get game by id
    public Games getGameById(Long id) {
        return gamesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + id));
    }
    //Search Game by title
    public List<Games> searchGames(String keyword) {
        return gamesRepository.findByTitleContainingIgnoreCase(keyword);
    }
    //find game by genre
    public List<Games> getGamesByGenre(String genre) {
        return gamesRepository.findByGenre(genre);
    }

    //Write operations
    //Add a game
    public Games addGame(Games game)
    {
        return gamesRepository.save(game);
    };

    //Delete Game records
    @Transactional
    public void deleteGames(Long id){
        if(!gamesRepository.existsById(id)){
            throw new RuntimeException("Game not found with id: " + id);
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
