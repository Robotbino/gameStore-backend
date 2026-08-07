package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Purchases;

import java.time.LocalDateTime;

/**
 * One owned game as the API promises it — decoupled from the Purchases entity,
 * the same pattern as UserResponse.from(Users).
 *
 * Why a DTO is mandatory, not stylistic: Purchases.user and Purchases.game are
 * both @ManyToOne(fetch = LAZY). Serializing the raw entity would drag the whole
 * Users record into the response and risk a LazyInitializationException the
 * moment Jackson touches a lazy field outside the transaction. This factory
 * reads only what it needs, while still inside the @Transactional service.
 *
 * Why the Games ENTITY is embedded rather than a GameResponse: Games.purchases
 * is already @JsonIgnore, so there is no serialization cycle, and the game comes
 * out byte-for-byte identical to /games/all — so the frontend's existing Game
 * type and GameGrid consume it unchanged. A dedicated GameResponse is a separate
 * roadmap item (B11); introducing it here would be an unrelated breaking change.
 */
public record PurchaseResponse(
        Long id,
        LocalDateTime purchaseDate,
        Games game
) {
    public static PurchaseResponse from(Purchases purchase) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseDate(),
                purchase.getGame()
        );
    }
}
