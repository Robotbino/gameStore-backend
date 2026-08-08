package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Games;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GamesRepository extends JpaRepository<Games, Long> {

    /**
     * One query backs three UX modes — plain list, keyword search, genre browse —
     * so the frontend stops downloading the entire catalogue to filter in the browser.
     *
     * Both filters are optional: null OR empty-string skips the clause. That means
     * /games/all with no params returns everything (paginated); ?q= only filters
     * by title; ?genre= only filters by genre; both narrows further.
     *
     * The old finder methods (findByGenre, findByTitleContainingIgnoreCase) that
     * this replaces were dead code — no controller ever reached them.
     */
    @Query("""
        SELECT g FROM Games g
        WHERE (:q IS NULL OR :q = '' OR LOWER(g.title) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:genre IS NULL OR :genre = '' OR g.genre = :genre)
        """)
    Page<Games> search(@Param("q") String q,
                       @Param("genre") String genre,
                       Pageable pageable);
}
