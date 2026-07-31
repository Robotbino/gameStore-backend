package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.GameRequest;
import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.service.GamesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GamesControllers {

    private final GamesService gamesService;

    @PostMapping("/add")
    public ResponseEntity<Games> addGame(@Valid @RequestBody GameRequest request) {
        Games newGame = gamesService.addGame(toEntity(request));
        return new ResponseEntity<>(newGame, HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Games>> getAllGames() {
        List<Games> games = gamesService.findAllGames();
        return new ResponseEntity<>(games, HttpStatus.OK);
    }

    @GetMapping("find/{id}")
    public ResponseEntity<Games> getGameById(@PathVariable("id") Long id) {
        Games games = gamesService.getGameById(id);
        return new ResponseEntity<>(games, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable("id") Long id) {
        gamesService.deleteGames(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Games> updateGame(@PathVariable("id") Long id, @Valid @RequestBody GameRequest request) {
        Games games = gamesService.updateGame(id, toEntity(request));
        return new ResponseEntity<>(games, HttpStatus.OK);
    }

    // Map the inbound DTO onto a transient entity the service can persist. rating
    // defaults to 0.0 (matching the entity default) when the request omits it.
    private Games toEntity(GameRequest request) {
        return Games.builder()
                .title(request.title())
                .genre(request.genre())
                .price(request.price())
                .rating(request.rating() != null ? request.rating() : 0.0)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .heroImage(request.heroImage())
                .build();
    }
}
