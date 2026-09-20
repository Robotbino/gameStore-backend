package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.RewardTransactions;
import com.gameStore.Bino.models.Users;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardTransactionRepository extends JpaRepository<RewardTransactions, Long> {
    @EntityGraph(attributePaths = "order")
    List<RewardTransactions> findTop20ByUserOrderByCreatedAtDescIdDesc(Users user);
}
