package com.gameStore.Bino.dto;

import com.gameStore.Bino.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound contract for admin user creation. Password is required here (unlike the
 * update DTO); role and points are optional and default in the controller.
 */
public record CreateUserRequest(
        @NotBlank(message = "userName is required")
        String userName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        Integer points,

        Role role
) {
}
