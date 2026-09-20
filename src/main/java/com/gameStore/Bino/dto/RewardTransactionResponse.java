package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.RewardTransactions;

import java.time.LocalDateTime;

public record RewardTransactionResponse(
        Long id,
        int delta,
        int balanceAfter,
        String reason,
        Long orderId,
        LocalDateTime createdAt
) {
    public static RewardTransactionResponse from(RewardTransactions tx) {
        return new RewardTransactionResponse(
                tx.getId(),
                tx.getDelta(),
                tx.getBalanceAfter(),
                tx.getReason().name(),
                tx.getOrder() != null ? tx.getOrder().getId() : null,
                tx.getCreatedAt()
        );
    }
}
