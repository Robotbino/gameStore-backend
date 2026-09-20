package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.WishlistResponse;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The signed-in user's wishlist. Identity always comes from the token; the
 * security chain's anyRequest().authenticated() already guards every route here.
 */
@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping("/me")
    public ResponseEntity<List<WishlistResponse>> myWishlist(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(wishlistService.findMine(user.getUsername()));
    }

    @PostMapping("/{gameId}")
    public ResponseEntity<WishlistResponse> add(
            @AuthenticationPrincipal Users user,
            @PathVariable Long gameId) {
        WishlistResponse entry = wishlistService.add(user.getUsername(), gameId);
        return new ResponseEntity<>(entry, HttpStatus.CREATED);
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal Users user,
            @PathVariable Long gameId) {
        wishlistService.remove(user.getUsername(), gameId);
        return ResponseEntity.noContent().build();
    }
}
