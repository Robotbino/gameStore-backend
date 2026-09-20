package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.CheckoutRequest;
import com.gameStore.Bino.dto.OrderResponse;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Simulated checkout and order history (roadmap B17). Identity always comes from the token. */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody CheckoutRequest request) {
        return new ResponseEntity<>(orderService.checkout(user.getUsername(), request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(orderService.findMyOrders(user.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> myOrder(@AuthenticationPrincipal Users user, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.findMyOrder(user.getUsername(), id));
    }
}
