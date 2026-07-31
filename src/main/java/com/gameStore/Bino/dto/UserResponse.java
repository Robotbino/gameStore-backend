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
    /**
     * NB: getUserName() (the entity field), NOT getUsername() — the latter is the
     * UserDetails override and returns the EMAIL. Mixing them up puts the email in
     * both fields and loses the real username entirely.
     *
     * role is mapped to String so API clients never depend on the Role enum.
     */
    public static UserResponse from(Users user) {
        return new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPoints()
        );
    }

    // QUIZ Q3: why a static factory instead of a constructor overload that
    // takes Users? (hint: what does a named method document that `new` can't,
    // and what would the canonical constructor still allow either way?)
}
