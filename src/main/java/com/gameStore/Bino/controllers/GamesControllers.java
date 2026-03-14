package com.gameStore.Bino.controllers;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.service.GamesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
public class GamesControllers {

@Autowired
private GamesService gamesService;

    @PostMapping("/add")
    public ResponseEntity<Games> addGame(@RequestBody Games game)
    {
        Games newGame = gamesService.addGame(game);
        return new ResponseEntity<>(newGame, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Games>> getAllGames()
    {
        List<Games> games = gamesService.findAllGames();
        return new ResponseEntity<>(games, HttpStatus.OK);
    }

    @GetMapping("find/{id}")
    public ResponseEntity<Games> getGameById(@PathVariable("id") Long id){
        Games games = gamesService.getGameById(id);
        return new ResponseEntity<>(games, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable("id") Long id) {
        gamesService.deleteGames(id);           // ← calls the service above
        return ResponseEntity.noContent().build();
    }

}
