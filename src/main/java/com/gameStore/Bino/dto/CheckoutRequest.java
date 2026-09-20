package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty List<Long> gameIds,
        @NotNull PaymentMethod paymentMethod,
        boolean redeemPoints
) {
}
