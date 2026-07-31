package com.gameStore.Bino.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gameStore.Bino.models.GenreDeserializer;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Inbound contract for creating/updating a game — the seam that keeps the JPA
 * entity out of the request body. A future RawgImportService maps RAWG JSON onto
 * THIS record instead of the entity, so the import path reuses the same mapping
 * and validation with no controller/service rewrite.
 *
 * rating is bounded 0.0–5.0 to match RAWG's scale. genre keeps the array-or-string
 * deserializer so both the frontend (["Action","RPG"]) and a plain "Action,RPG"
 * bind to the comma-separated column.
 */
public record GameRequest(
        @NotBlank(message = "title is required")
        String title,

        @JsonDeserialize(using = GenreDeserializer.class)
        String genre,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "rating must be between 0.0 and 5.0")
        @DecimalMax(value = "5.0", message = "rating must be between 0.0 and 5.0")
        Double rating,

        String description,

        @Size(max = 500, message = "imageUrl must be at most 500 characters")
        String imageUrl,

        @Size(max = 500, message = "heroImage must be at most 500 characters")
        String heroImage
) {
}
