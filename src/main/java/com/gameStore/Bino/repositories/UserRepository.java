package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Fixed: was JpaRepository<Users, Long> while Users.id is Integer — a generics
// mismatch Hibernate punishes at runtime ("provided id of the wrong type").
// Also removed the stray org.apache.catalina.User auto-import.
public interface UserRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    List<Users> findByUserNameContainingIgnoreCase(String keyword);

}
