package com.aayusheklavya.search.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Document stored in the products index; also the request body for indexing a product. */
public record Product(
        @NotBlank @Size(max = 40) String id,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotBlank String brand,
        @NotBlank String category,
        @NotNull @PositiveOrZero BigDecimal price,
        @DecimalMin("0.0") @DecimalMax("5.0") Double rating,
        @PositiveOrZero Integer reviewCount,
        boolean inStock,
        List<@NotBlank String> tags,
        LocalDate releasedOn) {
}
