package com.gameStore.Bino.dto;

import java.util.List;

/**
 * The result of a checkout, split into what was newly bought and what the user
 * already owned.
 *
 * This split is a deliberate stability choice, not a convenience: re-buying an
 * owned game is REPORTED, not thrown. That means the whole login → cart →
 * checkout → library flow can be run twice — a rehearsal and then the live
 * demo — without the second checkout erroring out. `alreadyOwned` carries the
 * ids the client asked for that were skipped because a purchase row already
 * existed (see PurchaseRepository.existsByUserAndGame).
 */
public record CheckoutResponse(
        List<PurchaseResponse> purchased,
        List<Long> alreadyOwned
) {
}
