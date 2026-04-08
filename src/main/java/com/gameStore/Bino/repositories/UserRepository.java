package com.gameStore.Bino.repositories;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Users;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users,Long>  {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);

    List<Users> findByUserNameContainingIgnoreCase(String keyword);

}
