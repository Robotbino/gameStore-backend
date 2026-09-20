package com.gameStore.Bino.service;

import com.gameStore.Bino.dto.CheckoutRequest;
import com.gameStore.Bino.dto.OrderResponse;
import com.gameStore.Bino.exceptions.InvalidCheckoutException;
import com.gameStore.Bino.exceptions.ResourceNotFoundException;
import com.gameStore.Bino.models.*;
import com.gameStore.Bino.repositories.GamesRepository;
import com.gameStore.Bino.repositories.OrderRepository;
import com.gameStore.Bino.repositories.PurchaseRepository;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Simulated checkout (roadmap B17). Creates the receipt, moves points, and
 * writes the ownership rows that PurchaseService would have written. The
 * server never declines a payment; the only client-visible failure is a cart
 * with nothing left to pay for.
 *
 * Re-loads the user by email for the same reason PurchaseService does: the
 * @AuthenticationPrincipal is detached, and points are mutated here.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERENCE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final PurchaseRepository purchaseRepository;
    private final GamesRepository gamesRepository;
    private final UserRepository userRepository;
    private final RewardsService rewardsService;

    @Transactional
    public OrderResponse checkout(String email, CheckoutRequest request) {
        Users user = loadUser(email);

        Set<Long> uniqueIds = new LinkedHashSet<>(request.gameIds());
        List<Games> payable = new ArrayList<>();
        List<Long> alreadyOwned = new ArrayList<>();

        for (Long gameId : uniqueIds) {
            Games game = gamesRepository.findById(gameId)
                    .orElseThrow(() -> new ResourceNotFoundException("Game not found with id: " + gameId));
            if (purchaseRepository.existsByUserAndGame(user, game)) {
                alreadyOwned.add(gameId);
            } else {
                payable.add(game);
            }
        }

        if (payable.isEmpty()) {
            throw new InvalidCheckoutException("Nothing to pay for: you already own every game in this cart");
        }

        BigDecimal subtotal = payable.stream()
                .map(Games::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = rewardsService.discountFor(
                rewardsService.balanceOf(user), subtotal, request.redeemPoints());
        BigDecimal total = subtotal.subtract(discount);
        int pointsRedeemed = rewardsService.pointsToRedeem(discount);
        int pointsEarned = rewardsService.pointsEarned(total);

        Orders order = Orders.builder()
                .user(user)
                .subtotal(subtotal)
                .discount(discount)
                .total(total)
                .pointsRedeemed(pointsRedeemed)
                .pointsEarned(pointsEarned)
                .paymentMethod(request.paymentMethod())
                .paymentReference(newReference())
                .status(OrderStatus.PAID)
                .build();
        for (Games game : payable) {
            order.addItem(OrderItems.builder()
                    .game(game)
                    .title(game.getTitle())
                    .unitPrice(game.getPrice())
                    .build());
        }
        order = orderRepository.save(order);

        if (pointsRedeemed > 0) {
            rewardsService.apply(user, order, -pointsRedeemed, RewardReason.REDEEM);
        }
        if (pointsEarned > 0) {
            rewardsService.apply(user, order, pointsEarned, RewardReason.EARN);
        }

        for (Games game : payable) {
            purchaseRepository.save(Purchases.builder().user(user).game(game).build());
        }

        return OrderResponse.from(order, rewardsService.balanceOf(user), alreadyOwned);
    }

    @Transactional
    public List<OrderResponse> findMyOrders(String email) {
        Users user = loadUser(email);
        int balance = rewardsService.balanceOf(user);
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(order -> OrderResponse.from(order, balance, List.of()))
                .toList();
    }

    @Transactional
    public OrderResponse findMyOrder(String email, Long id) {
        Users user = loadUser(email);
        Orders order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderResponse.from(order, rewardsService.balanceOf(user), List.of());
    }

    private Users loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    private static String newReference() {
        StringBuilder sb = new StringBuilder("DEMO-");
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            sb.append(REFERENCE_ALPHABET.charAt(RANDOM.nextInt(REFERENCE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
