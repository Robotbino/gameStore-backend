package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One checkout receipt. Column names and precisions must match
 * V5__orders_rewards.sql exactly — ddl-auto=validate refuses to start otherwise.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "points_redeemed", nullable = false)
    private int pointsRedeemed;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 32)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItems> items = new ArrayList<>();

    public void addItem(OrderItems item) {
        item.setOrder(this);
        items.add(item);
    }

    @Override
    public String toString() {
        return "Orders{id=" + id + ", total=" + total + ", reference=" + paymentReference + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orders other = (Orders) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
