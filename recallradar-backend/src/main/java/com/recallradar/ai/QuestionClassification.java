package com.recallradar.ai;

import java.time.LocalDate;

public record QuestionClassification(
        QuestionIntent intent,
        Long recallId,
        Long productId,
        String productName,
        String brand,
        String modelNumber,
        String batchNumber,
        String severity,
        String targetStatus,
        LocalDate dateFrom,
        LocalDate dateTo,
        String requestedOperation,
        boolean hasSpecificIdentifier,
        String clarificationPrompt
) {
    public QuestionClassification(QuestionIntent intent) {
        this(intent, null, null, null, null, null, null, null, null, null, null, null, false, null);
    }

    public static QuestionClassification of(QuestionIntent intent) {
        return new QuestionClassification(intent, null, null, null, null, null, null, null, null, null, null, null, false, null);
    }

    // 13-argument constructor preserving existing callers and tests (without productId)
    public QuestionClassification(
            QuestionIntent intent,
            Long recallId,
            String productName,
            String brand,
            String modelNumber,
            String batchNumber,
            String severity,
            String targetStatus,
            LocalDate dateFrom,
            LocalDate dateTo,
            String requestedOperation,
            boolean hasSpecificIdentifier,
            String clarificationPrompt
    ) {
        this(intent, recallId, null, productName, brand, modelNumber, batchNumber, severity, targetStatus, dateFrom, dateTo, requestedOperation, hasSpecificIdentifier, clarificationPrompt);
    }

    // 11-argument constructor with productId
    public QuestionClassification(
            QuestionIntent intent,
            Long recallId,
            Long productId,
            String productName,
            String brand,
            String modelNumber,
            String batchNumber,
            String severity,
            String targetStatus,
            boolean hasSpecificIdentifier,
            String clarificationPrompt
    ) {
        this(intent, recallId, productId, productName, brand, modelNumber, batchNumber, severity, targetStatus, null, null, null, hasSpecificIdentifier, clarificationPrompt);
    }

    // 10-argument constructor preserving existing callers and tests
    public QuestionClassification(
            QuestionIntent intent,
            Long recallId,
            String productName,
            String brand,
            String modelNumber,
            String batchNumber,
            String severity,
            String targetStatus,
            boolean hasSpecificIdentifier,
            String clarificationPrompt
    ) {
        this(intent, recallId, null, productName, brand, modelNumber, batchNumber, severity, targetStatus, null, null, null, hasSpecificIdentifier, clarificationPrompt);
    }

    // 7-argument constructor preserving existing callers and tests
    public QuestionClassification(
            QuestionIntent intent,
            Long recallId,
            String modelNumber,
            String batchNumber,
            String targetStatus,
            boolean hasSpecificIdentifier,
            String clarificationPrompt
    ) {
        this(intent, recallId, null, null, null, modelNumber, batchNumber, null, targetStatus, null, null, null, hasSpecificIdentifier, clarificationPrompt);
    }
}
