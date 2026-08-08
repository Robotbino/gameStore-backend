package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.GameRequest;
import com.gameStore.Bino.dto.GameResponse;
import com.gameStore.Bino.dto.PagedResponse;
import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.service.GamesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GamesControllers {

    private final GamesService gamesService;

    // Placeholder for the RAWG catalog sync — admin-only per SecurityConfiguration
    // (/games/** non-GET requires ROLE_ADMIN). Returns 501 so the demo can show the
    // full RBAC ladder (401 anonymous / 403 user / 501 admin) without committing to
    // an implementation that will be rewritten once RAWG ingestion lands.
    @PostMapping("/sync/rawg")
    public ResponseEntity<Map<String, String>> syncFromRawg() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "admin catalog sync — pending RAWG integration"));
    }

    @PostMapping("/add")
    public ResponseEntity<GameResponse> addGame(@Valid @RequestBody GameRequest request) {
        Games newGame = gamesService.addGame(toEntity(request));
        return new ResponseEntity<>(GameResponse.from(newGame), HttpStatus.CREATED);
    }

    /**
     * Paginated catalogue browse with optional keyword and genre filters. Any of
     * ?q= / ?genre= / ?page= / ?size= / ?sort=field,dir combine freely; a bare
     * GET /games/all returns page 0, 20 rows, sorted by id ASC.
     *
     * The default sort is deliberate: an unsorted paginated query returns rows
     * in DB order, which is stable for a single request but not across requests
     * — flipping page 2 → page 3 could show the same row twice or skip one.
     * Sorting by the id PK avoids that with zero server cost.
     *
     * The response is wrapped in PagedResponse (a small record we own) instead
     * of Spring Data's PageImpl — Spring Boot 3.3 explicitly warns that
     * PageImpl's JSON shape isn't a stable contract.
     */
    @GetMapping("/all")
    public ResponseEntity<PagedResponse<GameResponse>> getAllGames(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<Games> page = gamesService.search(q, genre, pageable);
        return ResponseEntity.ok(PagedResponse.from(page, GameResponse::from));
    }

    @GetMapping("find/{id}")
    public ResponseEntity<GameResponse> getGameById(@PathVariable("id") Long id) {
        Games games = gamesService.getGameById(id);
        return new ResponseEntity<>(GameResponse.from(games), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable("id") Long id) {
        gamesService.deleteGames(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameResponse> updateGame(@PathVariable("id") Long id,
                                                    @Valid @RequestBody GameRequest request) {
        Games games = gamesService.updateGame(id, toEntity(request));
        return new ResponseEntity<>(GameResponse.from(games), HttpStatus.OK);
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
