package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "games")
public class Games {
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Column(nullable = false)
    private String title;
    @Column
    private String genre;
    @Column(nullable = false)
    private BigDecimal price;
    @Column
    @Builder.Default
    private Double rating = 0.0;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "image_url")
    private String imageUrl;
    @Column(name = "hero_image")
    private String heroImage;




    public String toString()
    {
        return "Games{"+
                "id ="+ id +
                ", title='" + title + '\'' +
                ", price='" + price + '\'' +
                ", genre='" + genre + '\'' +
                ", description='" + description + '\'' +
                ", rating='" + rating + '\'' +
                ", heroImage='" + heroImage + '\'' +
                ", imageUrl='" + imageUrl+ '\'' +
                '}';
    }
}
