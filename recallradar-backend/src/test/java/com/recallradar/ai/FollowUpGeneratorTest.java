package com.recallradar.ai;

import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpGeneratorTest {

    private FollowUpGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new FollowUpGenerator();
    }

    @Test
    void testGenerateFollowUpsForGreeting() {
        QuestionClassification classification = new QuestionClassification(QuestionIntent.GREETING_OR_CHITCHAT);
        List<String> followUps = generator.generateFollowUps(classification, "Hi", List.of(), "Hello!");

        assertNotNull(followUps);
        assertFalse(followUps.isEmpty());
        assertTrue(followUps.stream().anyMatch(s -> s.contains("check if a product has been recalled")));
    }

    @Test
    void testGenerateFollowUpsForDatabaseStatistics() {
        QuestionClassification classification = new QuestionClassification(QuestionIntent.DATABASE_STATISTICS);
        List<String> followUps = generator.generateFollowUps(classification, "How many recalls?", List.of(), "Stats...");

        assertNotNull(followUps);
        assertTrue(followUps.stream().anyMatch(s -> s.contains("latest recalls")));
    }

    @Test
    void testGenerateFollowUpsForGeneralSafetyAdvice() {
        QuestionClassification classification = new QuestionClassification(QuestionIntent.GENERAL_SAFETY_ADVICE);
        List<String> followUps = generator.generateFollowUps(classification, "What should I check before buying?", List.of(), "Advice...");

        assertNotNull(followUps);
        assertTrue(followUps.stream().anyMatch(s -> s.contains("model and batch number")));
    }

    @Test
    void testGenerateFollowUpsForSingleSpecificRecall() {
        Product product = new Product();
        product.setBrand("Bosch");
        product.setModelNumber("BSH-500");

        Recall recall = new Recall();
        recall.setId(301L);
        recall.setProduct(product);
        recall.setSourceUrl("https://example.com/recall/301");

        QuestionClassification classification = new QuestionClassification(QuestionIntent.SPECIFIC_RECALL_LOOKUP);
        List<String> followUps = generator.generateFollowUps(classification, "Is Bosch BSH-500 recalled?", List.of(recall), "Yes, Recall ID 301...");

        assertNotNull(followUps);
        assertTrue(followUps.stream().anyMatch(s -> s.contains("Recall ID #301")));
        assertTrue(followUps.stream().anyMatch(s -> s.contains("Bosch")));
    }

    @Test
    void testGenerateFollowUpsForMultipleRecalls() {
        Recall r1 = new Recall();
        r1.setId(10L);
        Recall r2 = new Recall();
        r2.setId(20L);

        QuestionClassification classification = new QuestionClassification(QuestionIntent.SPECIFIC_RECALL_LOOKUP);
        List<String> followUps = generator.generateFollowUps(classification, "DeWalt recalls", List.of(r1, r2), "Found 2 recalls...");

        assertNotNull(followUps);
        assertTrue(followUps.stream().anyMatch(s -> s.contains("Recall ID #10")));
        assertTrue(followUps.stream().anyMatch(s -> s.contains("highest severity")));
    }

    @Test
    void testGenerateFollowUpsWhenNoRecallsMatch() {
        QuestionClassification classification = new QuestionClassification(QuestionIntent.SPECIFIC_RECALL_LOOKUP);
        List<String> followUps = generator.generateFollowUps(classification, "Is unknown gizmo recalled?", List.of(), "No matching recall records...");

        assertNotNull(followUps);
        assertTrue(followUps.stream().anyMatch(s -> s.contains("SaferProducts.gov")));
    }
}
