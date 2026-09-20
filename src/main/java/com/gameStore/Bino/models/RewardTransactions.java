package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/** One movement in the points ledger. delta is positive for EARN, negative for REDEEM. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reward_transactions")
public class RewardTransactions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order;

    @Column(nullable = false)
    private int delta;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardReason reason;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public String toString() {
        return "RewardTransactions{id=" + id + ", delta=" + delta + ", reason=" + reason + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RewardTransactions other = (RewardTransactions) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
