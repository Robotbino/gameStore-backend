package com.gameStore.Bino.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound contract for PUT /users/me/account — the caller's own identity.
 * Both fields are unique in the database, so the service checks availability
 * excluding the caller's own row before saving; without that exclusion,
 * saving your profile without changing your email would report your own
 * address as taken.
 *
 * Validation messages match UpdateUserRequest's wording so the frontend's
 * field-error handling sees one vocabulary across admin and self-service.
 */
public record UpdateAccountRequest(
        @NotBlank(message = "userName is required")
        String userName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email
) {
}
