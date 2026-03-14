package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
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
        return "purchases{"+
            "id ="+ id +
            ", user Id='" + user+ '\'' +
            ", game Id='" + game + '\'' +
            ", purchase Date='" + purchaseDate + '\'' +
            '}';
    }
}
