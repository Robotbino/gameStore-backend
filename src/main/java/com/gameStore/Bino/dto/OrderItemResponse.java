package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.OrderItems;

import java.math.BigDecimal;

/** gameId and imageUrl are null when the game has since been deleted; the snapshot still stands. */
public record OrderItemResponse(
        Long gameId,
        String title,
        BigDecimal unitPrice,
        String imageUrl
) {
    public static OrderItemResponse from(OrderItems item) {
        Games game = item.getGame();
        return new OrderItemResponse(
                game != null ? game.getId() : null,
                item.getTitle(),
                item.getUnitPrice(),
                game != null ? game.getImageUrl() : null
        );
    }
}
