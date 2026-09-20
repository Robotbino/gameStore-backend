package com.gameStore.Bino.dto;

import jakarta.validation.constraints.NotBlank;

/** Self-service profile edit. Only the username is editable; email, role and points are not. */
public record UpdateProfileRequest(
        @NotBlank(message = "userName is required")
        String userName
) {
}
