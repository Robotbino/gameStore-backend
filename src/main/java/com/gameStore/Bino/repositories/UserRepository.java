package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<Users,Integer>  {

    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);

}
