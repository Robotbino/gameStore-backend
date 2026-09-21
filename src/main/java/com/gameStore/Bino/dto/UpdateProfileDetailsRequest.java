package com.gameStore.Bino.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound contract for PUT /users/me/profile — the caller's presentation
 * fields (V6 columns). Every field is optional and NULL/blank REPLACES the
 * stored value, which is what makes "clear my bio" possible. This is the
 * opposite of UpdateUserRequest's null-means-keep, on purpose.
 *
 * Identity (userName, email) lives on UpdateAccountRequest and the username
 * shortcut on UpdateProfileRequest; role and points have no component here,
 * so Jackson drops them and a caller cannot promote themselves.
 *
 * Sizes mirror the column widths so a too-long value is a 400 with a field
 * error rather than a truncation exception from the database.
 */
public record UpdateProfileDetailsRequest(
        @Size(max = 50, message = "displayName must be at most 50 characters")
        String displayName,

        @Size(max = 32, message = "avatarKey must be at most 32 characters")
        String avatarKey,

        @Size(max = 280, message = "bio must be at most 280 characters")
        String bio,

        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "country must be an ISO 3166-1 alpha-2 code")
        String country
) {
}
