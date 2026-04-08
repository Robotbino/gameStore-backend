package com.gameStore.Bino.service;

import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class UsersService {
    private final UserRepository userRepository;

    public List<Users> findAllUsers()
    {
        return userRepository.findAll();
    }

    public Users findUserByID(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: "+ id));
    }
    //Search Game by title
    public List<Users> searchGames(String keyword) {
        return userRepository.findByUserNameContainingIgnoreCase(keyword);
    }

    //Add a User
    public Users addUser(Users user)
    {
        return userRepository.save(user);
    };

    //Delete User record
    @Transactional
    public void deleteUser(Long id){
        if(!userRepository.existsById(id)){
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public Users updateUser(Long id, Users updatedUser) {
        Users existing = findUserByID(id);  // throws if not found

        existing.setUserName(updatedUser.getUsername());
        existing.setEmail(updatedUser.getEmail());
        existing.setPoints(updatedUser.getPoints());
        existing.setPassword(updatedUser.getPassword());
        existing.setPurchases(updatedUser.getPurchases());
        existing.setRole(updatedUser.getRole());


        return userRepository.save(existing);
    }
}
