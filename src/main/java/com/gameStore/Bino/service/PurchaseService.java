package com.gameStore.Bino.service;

import com.gameStore.Bino.dto.CheckoutResponse;
import com.gameStore.Bino.dto.PurchaseResponse;
import com.gameStore.Bino.exceptions.ResourceNotFoundException;
import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Purchases;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.GamesRepository;
import com.gameStore.Bino.repositories.PurchaseRepository;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Purchase + library logic. Wires together the Purchases entity and
 * PurchaseRepository, which existed but were injected nowhere until now
 * (roadmap B9).
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final GamesRepository gamesRepository;
    private final UserRepository userRepository;

    /**
     * Buy the given games for the user identified by email.
     *
     * Why email, not the Users principal object: the @AuthenticationPrincipal
     * handed to the controller is a DETACHED entity — JWTAuthenticationFilter
     * loads it via userDetailsService outside any transaction (UsersController
     * documents the same trap). Re-reading it here binds a managed instance to
     * this transaction, so purchaseRepository.save and existsByUserAndGame both
     * operate on an attached entity.
     *
     * gameIds are de-duplicated first, so a cart that somehow holds the same
     * game twice can never create two rows. Games already owned are collected
     * into alreadyOwned instead of throwing, so a repeat checkout is a no-op
     * report rather than an error.
     */
    @Transactional
    public CheckoutResponse checkout(String email, List<Long> gameIds) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

        // Preserve request order, drop duplicates.
        Set<Long> uniqueIds = new LinkedHashSet<>(gameIds);

        List<PurchaseResponse> purchased = new ArrayList<>();
        List<Long> alreadyOwned = new ArrayList<>();

        for (Long gameId : uniqueIds) {
            Games game = gamesRepository.findById(gameId)
                    .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));

            if (purchaseRepository.existsByUserAndGame(user, game)) {
                alreadyOwned.add(gameId);
                continue;
            }

            Purchases saved = purchaseRepository.save(
                    Purchases.builder()
                            .user(user)
                            .game(game)
                            .build()
            );
            purchased.add(PurchaseResponse.from(saved));
        }

        return new CheckoutResponse(purchased, alreadyOwned);
    }

    /**
     * Every game the user owns, most-recent first. Runs in a transaction so the
     * lazy game association is initialized before PurchaseResponse.from reads it.
     */
    @Transactional
    public List<PurchaseResponse> findMyPurchases(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));

        return purchaseRepository.findByUser(user).stream()
                .sorted((a, b) -> b.getPurchaseDate().compareTo(a.getPurchaseDate()))
                .map(PurchaseResponse::from)
                .toList();
    }
}
