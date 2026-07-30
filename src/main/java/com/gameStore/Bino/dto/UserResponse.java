package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Users;

/**
 * What the API promises, decoupled from what the DB stores (your A8 — SRP:
 * the API contract and the persistence model change for different reasons).
 *
 * A record gives you: private final fields, canonical constructor, accessors,
 * equals/hashCode/toString — immutability for free (Java 17).
 *
 * The fields below are the WHOLE contract. What's absent from this list
 * IS the security fix — the password hash never leaves the service layer again.
 */
public record UserResponse(
        Integer id,
        String userName,
        String email,
        String role,
        Integer points
) {
    // TODO 0 — static factory (encapsulation, your 05 §1):
    //   public static UserResponse from(Users user) { ... }
    public static UserResponse from (Users users){
        return new UserResponse(users.getId(),users.getUsername(), users.getEmail(),users.getRole().name(),users.getPoints());
    };
    // Map field by field. For role use user.getRole().name() — keeping it a
    // String means API clients never depend on your enum.
    //
    // QUIZ Q3: why a static factory instead of a constructor overload that
    // takes Users? (hint: what does a named method document that `new` can't,
    // and what would the canonical constructor still allow either way?)
}
