package com.gameStore.Bino.service;

import com.gameStore.Bino.dto.WishlistResponse;
import com.gameStore.Bino.exceptions.ResourceNotFoundException;
import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.models.Wishlists;
import com.gameStore.Bino.repositories.GamesRepository;
import com.gameStore.Bino.repositories.UserRepository;
import com.gameStore.Bino.repositories.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final GamesRepository gamesRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<WishlistResponse> findMine(String email) {
        return wishlistRepository.findByUserOrderByAddedAtDesc(managedUser(email)).stream()
                .map(WishlistResponse::from)
                .toList();
    }

    /** Idempotent: wishlisting a game already on the list returns the existing entry. */
    @Transactional
    public WishlistResponse add(String email, Long gameId) {
        Users user = managedUser(email);
        Games game = findGame(gameId);

        Wishlists entry = wishlistRepository.findByUserAndGame(user, game)
                .orElseGet(() -> wishlistRepository.save(
                        Wishlists.builder().user(user).game(game).build()));
        return WishlistResponse.from(entry);
    }

    /** Removing a game that isn't on the list is a no-op, so the client can always send DELETE. */
    @Transactional
    public void remove(String email, Long gameId) {
        Users user = managedUser(email);
        Games game = findGame(gameId);
        wishlistRepository.findByUserAndGame(user, game).ifPresent(wishlistRepository::delete);
    }

    // The @AuthenticationPrincipal is detached (loaded by the JWT filter outside any
    // transaction), so re-read the user here to get an instance bound to this one.
    private Users managedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    private Games findGame(Long gameId) {
        return gamesRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));
    }
}
