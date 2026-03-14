package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Games;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface GamesRepository extends JpaRepository<Games,Long> {

    // Custom query: find games by genre
    List<Games> findByGenre(String genre);


    // Search games where title contains a keyword (case-insensitive)
    List<Games> findByTitleContainingIgnoreCase(String keyword);
}
