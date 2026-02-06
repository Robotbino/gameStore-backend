package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.AuthUsers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepo extends JpaRepository<AuthUsers,Integer>  {

    Optional<AuthUsers> findByEmail(String email);

}
