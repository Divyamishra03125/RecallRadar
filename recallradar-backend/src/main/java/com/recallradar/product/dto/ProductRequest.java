package com.recallradar.product.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        String name,

        @Size(max = 150)
        String brand,

        @Size(max = 100)
        String category,

        @Size(max = 100)
        String modelNumber,

        @Size(max = 100)
        String batchNumber
) {}
