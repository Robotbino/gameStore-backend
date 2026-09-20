package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.models.Wishlists;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlists, Long> {

    // Fetch-join the game: WishlistResponse serializes it after the transaction ends.
    @EntityGraph(attributePaths = "game")
    List<Wishlists> findByUserOrderByAddedAtDesc(Users user);

    @EntityGraph(attributePaths = "game")
    Optional<Wishlists> findByUserAndGame(Users user, Games game);
}
