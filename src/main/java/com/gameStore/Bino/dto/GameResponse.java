package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Games;

import java.math.BigDecimal;

/**
 * API contract for /games, decoupled from the JPA entity — mirrors UserResponse
 * (records + static factory) so the two resources shape the same way at the edge.
 *
 * Absent by design: `purchases` (the LAZY back-reference from Games). Serialising
 * a raw Games entity to JSON reaches that collection and either fires N+1 or
 * throws LazyInitializationException outside a transaction. Exposing THIS record
 * makes the omission explicit — you can't accidentally leak an association you
 * never wrote down.
 */
public record GameResponse(
        Long id,
        String title,
        String genre,
        BigDecimal price,
        Double rating,
        String description,
        String imageUrl,
        String heroImage
) {
    public static GameResponse from(Games g) {
        return new GameResponse(
                g.getId(),
                g.getTitle(),
                g.getGenre(),
                g.getPrice(),
                g.getRating(),
                g.getDescription(),
                g.getImageUrl(),
                g.getHeroImage()
        );
    }
}
