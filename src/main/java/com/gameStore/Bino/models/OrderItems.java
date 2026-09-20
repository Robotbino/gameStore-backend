package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A line on a receipt. title and unitPrice are snapshots taken at checkout;
 * game is a nullable back-reference that survives the game being deleted.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Games game;

    @Column(nullable = false)
    private String title;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Override
    public String toString() {
        return "OrderItems{id=" + id + ", title='" + title + "', unitPrice=" + unitPrice + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItems other = (OrderItems) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
