package com.gameStore.Bino.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Checkout payload: the ids of the games the cart is buying in one go.
 *
 * A list (not a single id) so one checkout is one transaction — the frontend
 * cart submits its whole contents at once instead of N sequential POSTs, each
 * of which could half-fail and leave the cart in an ambiguous state.
 *
 * The identity of the BUYER is never in this body — it comes from the JWT via
 * @AuthenticationPrincipal in the controller. Trusting a userId from the client
 * would be the same IDOR that UsersController documents avoiding.
 */
public record PurchaseRequest(
        @NotEmpty(message = "At least one game id is required")
        List<Long> gameIds
) {
}
