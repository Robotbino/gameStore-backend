package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Fixed: was JpaRepository<Users, Long> while Users.id is Integer — a generics
// mismatch Hibernate punishes at runtime ("provided id of the wrong type").
// Also removed the stray org.apache.catalina.User auto-import.
public interface UserRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    /**
     * Paginated user listing with optional username keyword filter. Backs
     * /users/all — a null OR empty q returns everything, a value narrows by
     * case-insensitive contains on userName.
     *
     * Replaces the old List-returning findByUserNameContainingIgnoreCase
     * (dead code — no controller consumed it) and the unbounded findAll().
     */
    @Query("""
        SELECT u FROM Users u
        WHERE (:q IS NULL OR :q = '' OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Users> search(@Param("q") String q, Pageable pageable);
}
