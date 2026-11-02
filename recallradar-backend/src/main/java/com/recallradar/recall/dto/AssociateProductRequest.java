package com.recallradar.recall.dto;

import jakarta.validation.constraints.Pattern;

public record AssociateProductRequest(
        Long productId,

        @Pattern(regexp = "(?i)^(LINKED|UNLINKED|PENDING_REVIEW)$", message = "Association status must be LINKED, UNLINKED, or PENDING_REVIEW")
        String associationStatus,

        String notes
) {}
