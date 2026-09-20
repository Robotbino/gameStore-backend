package com.gameStore.Bino.service;

import com.gameStore.Bino.dto.RewardTransactionResponse;
import com.gameStore.Bino.dto.RewardsSummaryResponse;
import com.gameStore.Bino.models.Orders;
import com.gameStore.Bino.models.RewardReason;
import com.gameStore.Bino.models.RewardTransactions;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.RewardTransactionRepository;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single source of truth for points maths. The frontend mirrors these
 * rules in src/utils/rewards.ts for previews, but the server always recomputes.
 *
 *   earn:   10 points per R1 of the final total, rounded down
 *   redeem: 100 points = R1 off, capped at the subtotal
 */
@Service
@RequiredArgsConstructor
public class RewardsService {

    public static final int POINTS_PER_RAND_EARNED = 10;
    public static final int POINTS_PER_RAND_OFF = 100;

    private final RewardTransactionRepository rewardTransactionRepository;
    private final UserRepository userRepository;

    /** Rands off for this balance and subtotal; zero when not redeeming or nothing to redeem. */
    public BigDecimal discountFor(int balance, BigDecimal subtotal, boolean redeem) {
        if (!redeem || balance <= 0) return money(BigDecimal.ZERO);
        BigDecimal worth = randsFor(balance);
        return money(worth.min(subtotal));
    }

    public int pointsToRedeem(BigDecimal discount) {
        return discount.multiply(BigDecimal.valueOf(POINTS_PER_RAND_OFF)).intValueExact();
    }

    public int pointsEarned(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(POINTS_PER_RAND_EARNED))
                .setScale(0, RoundingMode.DOWN)
                .intValueExact();
    }

    public BigDecimal randsFor(int points) {
        return money(BigDecimal.valueOf(points)
                .divide(BigDecimal.valueOf(POINTS_PER_RAND_OFF), 2, RoundingMode.DOWN));
    }

    /**
     * Moves the user's balance and records the ledger row. Caller must hold a
     * transaction and pass a managed Users instance.
     */
    public RewardTransactions apply(Users user, Orders order, int delta, RewardReason reason) {
        int newBalance = balanceOf(user) + delta;
        user.setPoints(newBalance);
        return rewardTransactionRepository.save(RewardTransactions.builder()
                .user(user)
                .order(order)
                .delta(delta)
                .balanceAfter(newBalance)
                .reason(reason)
                .build());
    }

    public int balanceOf(Users user) {
        return user.getPoints() == null ? 0 : user.getPoints();
    }

    @Transactional
    public RewardsSummaryResponse summaryFor(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        int balance = balanceOf(user);
        return new RewardsSummaryResponse(
                balance,
                randsFor(balance),
                rewardTransactionRepository.findTop20ByUserOrderByCreatedAtDescIdDesc(user).stream()
                        .map(RewardTransactionResponse::from)
                        .toList()
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.DOWN);
    }
}
