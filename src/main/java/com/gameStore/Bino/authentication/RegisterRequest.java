package com.gameStore.Bino.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

        private String title;
        private String genre;
        private BigDecimal price;
        private String description;
        private String heroImage;
        private Double rating;
}
