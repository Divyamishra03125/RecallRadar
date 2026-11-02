package com.recallradar.product.dto;



import java.time.OffsetDateTime;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        String category,
        String modelNumber,
        String batchNumber,
        OffsetDateTime createdAt
) {}
