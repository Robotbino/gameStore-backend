package com.gameStore.Bino.controllers;

import com.gameStore.Bino.models.Games;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.GamesService;
import com.gameStore.Bino.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Controller
@RequestMapping("/users")
public class UsersController {
    @Autowired
    public UsersService usersService;

    @GetMapping("/all")
    public ResponseEntity<List<Users>> getAllUsers()
    {
        List<Users> users = usersService.findAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        usersService.deleteUser(id);           // ← calls the service above
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add")
    public ResponseEntity<Users> addUser(@RequestBody Users user)
    {
        Users newUser = usersService.addUser(user);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable("id")Long id,@RequestBody Users users) {
        Users user = usersService.updateUser(id, users);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

}
