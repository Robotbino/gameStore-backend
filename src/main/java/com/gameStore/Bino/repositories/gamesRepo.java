package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Games;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Repository
public interface gamesRepo extends JpaRepository<Games,Long> {
    void deleteGameById(Long id);

    @Override
    Optional<Games> findById(Long id);
}
