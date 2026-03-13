package com.gameStore.Bino.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class purchases {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private String id;
    private String user_id;
    private String game_id;
    private LocalDateTime purchaseDate;


    @Override
    public String toString(){
        return "purchases{"+
            "id ="+ id +
            ", user Id='" + user_id + '\'' +
            ", game Id='" + game_id + '\'' +
            ", purchase Date='" + purchaseDate + '\'' +
            '}';
    }
}
