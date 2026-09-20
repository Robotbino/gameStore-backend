package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Orders;
import com.gameStore.Bino.models.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    // Fetch items and their games in one go so OrderResponse.from can read
    // them without lazy-loading per row.
    @EntityGraph(attributePaths = {"items", "items.game"})
    List<Orders> findByUserOrderByCreatedAtDesc(Users user);

    @EntityGraph(attributePaths = {"items", "items.game"})
    Optional<Orders> findByIdAndUser(Long id, Users user);
}
