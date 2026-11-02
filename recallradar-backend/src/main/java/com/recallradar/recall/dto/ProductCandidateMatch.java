package com.recallradar.recall.dto;

public record ProductCandidateMatch(
        Long productId,
        String productName,
        String brand,
        String category,
        String modelNumber,
        String batchNumber,
        int confidenceScore,
        String matchReason
) {}
