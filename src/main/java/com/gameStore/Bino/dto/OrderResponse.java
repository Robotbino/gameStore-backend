package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A receipt as the API promises it. pointsBalance is the user's balance after
 * this order; alreadyOwned lists cart ids that were skipped at checkout and is
 * empty when reading historic orders.
 */
public record OrderResponse(
        Long id,
        BigDecimal subtotal,
        int pointsRedeemed,
        BigDecimal discount,
        BigDecimal total,
        int pointsEarned,
        int pointsBalance,
        String paymentMethod,
        String paymentReference,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        List<Long> alreadyOwned
) {
    public static OrderResponse from(Orders order, int pointsBalance, List<Long> alreadyOwned) {
        return new OrderResponse(
                order.getId(),
                order.getSubtotal(),
                order.getPointsRedeemed(),
                order.getDiscount(),
                order.getTotal(),
                order.getPointsEarned(),
                pointsBalance,
                order.getPaymentMethod().name(),
                order.getPaymentReference(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                alreadyOwned
        );
    }
}
