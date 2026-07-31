package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound contract for admin user edits. password is OPTIONAL — the frontend omits
 * it on a normal edit, and UsersService.updateUser keeps the existing hash when it's
 * blank. @Size still applies when a value IS supplied (null passes @Size), so a new
 * password must clear the same length bar as registration.
 */
public record UpdateUserRequest(
        @NotBlank(message = "userName is required")
        String userName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        Integer points,

        Role role
) {
}
