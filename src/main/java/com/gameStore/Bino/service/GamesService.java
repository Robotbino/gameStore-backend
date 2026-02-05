package com.gameStore.Bino.service;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.repositories.gamesRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class GamesService {
    private final gamesRepo gamesRepo;

    public Games addGame(Games game)
    {
        return gamesRepo.save(game);
    };

    public List<Games> findAllGames()
    {
        return gamesRepo.findAll();
    }

    //Update Game records
    public Games updateGames(Games games){
        return gamesRepo.save(games);
    }
    //Delete Game records

    //Delete Game records
    @Transactional
    public void deleteGames(Long id){
        gamesRepo.deleteGameById(id);
    }
    @Transactional
    public Games findGamesById(Long id) {
        return gamesRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User by id" + id + "was not found"));
    }
}
