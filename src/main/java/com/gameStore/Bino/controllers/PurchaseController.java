package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.CheckoutResponse;
import com.gameStore.Bino.dto.PurchaseRequest;
import com.gameStore.Bino.dto.PurchaseResponse;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Purchase + library endpoints (roadmap B9).
 *
 * No entry is needed in SecurityConfiguration: its chain ends in
 * .anyRequest().authenticated(), so both routes require a valid JWT the moment
 * they exist. Identity always comes from @AuthenticationPrincipal — the one
 * part of the request the client cannot forge — never from the body or a param.
 */
@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    // POST /purchases — buy the games in the request for the authenticated user.
    // Returns 201 with {purchased, alreadyOwned}; re-buying an owned game lands
    // in alreadyOwned rather than failing the request.
    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody PurchaseRequest request) {
        CheckoutResponse response = purchaseService.checkout(user.getUsername(), request.gameIds());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET /purchases/me — the caller's library, identified by the token.
    @GetMapping("/me")
    public ResponseEntity<List<PurchaseResponse>> myPurchases(
            @AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(purchaseService.findMyPurchases(user.getUsername()));
    }
}
