package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Wishlists;

import java.time.LocalDateTime;

/** One wishlisted game. Embeds the Games entity for the same reasons PurchaseResponse does. */
public record WishlistResponse(
        Long id,
        LocalDateTime addedAt,
        Games game
) {
    public static WishlistResponse from(Wishlists entry) {
        return new WishlistResponse(entry.getId(), entry.getAddedAt(), entry.getGame());
    }
}
