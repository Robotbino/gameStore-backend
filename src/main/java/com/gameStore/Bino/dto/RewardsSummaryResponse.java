package com.gameStore.Bino.dto;

import java.math.BigDecimal;
import java.util.List;

/** balance in points; worth is the same balance expressed in Rands at the redemption rate. */
public record RewardsSummaryResponse(
        int balance,
        BigDecimal worth,
        List<RewardTransactionResponse> transactions
) {
}
