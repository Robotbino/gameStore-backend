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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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


    // ---------------------------------------------------------------
    // Relationship: One Game -> Many Purchases
    // A game can appear in many purchase records.
    // ---------------------------------------------------------------
    @OneToMany(mappedBy = "games")
    @Builder.Default
    private List<Purchases> purchases = new ArrayList<>();

    @Override
    public String toString() {
        // Exclude 'purchases' to avoid infinite recursion with Lombok's @Data
        return "Game{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", price=" + price +
                ", rating=" + rating +
                '}';
    }
}
