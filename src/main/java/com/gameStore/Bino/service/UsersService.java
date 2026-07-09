package com.gameStore.Bino.service;

import com.gameStore.Bino.exceptions.ResourceNotFoundException;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class UsersService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Users> findAllUsers()
    {
        return userRepository.findAll();
    }

    public Users findUserByID(Integer id){
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+ id));
    }
    //Search Game by title
    public List<Users> searchGames(String keyword) {
        return userRepository.findByUserNameContainingIgnoreCase(keyword);
    }

    //Add a User
    public Users addUser(Users user)
    {
        if(userRepository.existsByEmail(user.getEmail())){
            throw new RuntimeException("Email already in use");
        }
        if(userRepository.existsByUserName(user.getUserName())){
            throw new RuntimeException("Username already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    };

    //Delete User record
    @Transactional
    public void deleteUser(Integer id){
        if(!userRepository.existsById(id)){
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public Users updateUser(Integer id, Users updatedUser) {
        Users existing = findUserByID(id);  // throws if not found

        existing.setUserName(updatedUser.getUserName());
        existing.setEmail(updatedUser.getEmail());
        existing.setPoints(updatedUser.getPoints());
        //The frontend omits the password on edits; only replace it when a new one is provided
        if(updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()){
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        existing.setRole(updatedUser.getRole());

        return userRepository.save(existing);
    }
}
