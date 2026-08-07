package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Purchases;
import com.gameStore.Bino.models.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchases, Long> {
    // Fetch-join the game so it comes back as a real entity, not a lazy proxy.
    // The library response (PurchaseResponse.from) serializes game outside the
    // transaction, so an uninitialized proxy would fail as a HibernateProxy.
    @EntityGraph(attributePaths = "game")
    List<Purchases> findByUser(Users user);

    // Get all purchases of a specific game
    List<Purchases> findByGame(Games game);

    // Check if a user already owns a game (prevent duplicate purchases)
    boolean existsByUserAndGame(Users user, Games game);
}