package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.UserResponse;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Was @Controller — it only worked because every handler returns ResponseEntity.
// QUIZ Q1: what would a handler returning a plain List<Users> have done under @Controller?
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor // DIP (your A9): constructor injection on a final field — same style as your services
public class UsersController {

    private final UsersService usersService;

    // ============================================================
    // TODO 1 — GET /users/me  (the point of Block 1)
    // ============================================================
    // Signature:
    //   @GetMapping("/me")
    //   public ResponseEntity<UserResponse> getMe(Authentication authentication)
    //
    // Steps:
    //   1. authentication.getPrincipal() returns Object — it's the UserDetails
    //      your JWTAuthenticationFilter placed in the SecurityContext (you wrote
    //      that setAuthentication(...) line). Cast it to Users.
    //   2. Return 200 with UserResponse.from(...).
    //
    // QUIZ Q2: why must the current user come from the token and never from a
    //          ?userId= param? (your plan answers this — one sentence, out loud)

    // TODO 2 — map entities -> DTOs here too. Your 05 §4: users.stream().map(...).toList()
    // That single change stops the hash leak AND defuses the Jackson cycle.
    // Path stays /all for now — the frontend calls it.
    @GetMapping("/all")
    public ResponseEntity<List<Users>> getAllUsers()
    {
        List<Users> users = usersService.findAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Integer id) {
        usersService.deleteUser(id);           // ← calls the service above
        return ResponseEntity.noContent().build();
    }

    // TODO 3 — return the DTO instead of the entity, still 201 CREATED
    @PostMapping("/add")
    public ResponseEntity<Users> addUser(@RequestBody Users user)
    {
        Users newUser = usersService.addUser(user);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    // TODO 4 — same DTO conversion, 200 OK
    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable("id") Integer id, @RequestBody Users users) {
        Users user = usersService.updateUser(id, users);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

}
