package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table( name ="purchases")
public class Purchases {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Games game;

    @Column(name = "purchase_date", nullable = false)
    @Builder.Default
    private LocalDateTime purchaseDate = LocalDateTime.now();


    @Override
    public String toString(){
        return "Purchases{"+
            "id=" + id +
            ", userId=" + (user != null ? user.getId() : null) +
            ", gameId=" + (game != null ? game.getId() : null) +
            ", purchaseDate=" + purchaseDate +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Purchases purchases = (Purchases) o;
        return id != null && Objects.equals(id, purchases.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
