package com.gameStore.Bino.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "games")
public class Games {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    //Stored as a comma-separated string; the frontend may send it as an array
    @Column
    @JsonDeserialize(using = GenreDeserializer.class)
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
    @OneToMany(mappedBy = "game")
    @Builder.Default
    @JsonIgnore
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Games games = (Games) o;
        return id != null && Objects.equals(id, games.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
