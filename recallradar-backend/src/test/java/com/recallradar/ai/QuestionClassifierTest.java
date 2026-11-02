package com.recallradar.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class QuestionClassifierTest {

    private QuestionClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new QuestionClassifier();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "hello",
            "Hi",
            "hey there",
            "Good morning",
            "what's up"
    })
    void testClassifyGreetingAndChitchat(String question) {
        QuestionClassification result = classifier.classify(question);
        assertEquals(QuestionIntent.GREETING_OR_CHITCHAT, result.intent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "who are you?",
            "what can you do",
            "help",
            "what is RecallRadar?"
    })
    void testClassifyHelpAndCapabilities(String question) {
        QuestionClassification result = classifier.classify(question);
        assertTrue(result.intent() == QuestionIntent.APPLICATION_HELP || result.intent() == QuestionIntent.HELP_AND_CAPABILITIES);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "how many recalls are in the database?",
            "total recalls",
            "how many active recalls do you have?",
            "database statistics",
            "count of recalls"
    })
    void testClassifyDashboardAnalysis(String question) {
        QuestionClassification result = classifier.classify(question);
        assertTrue(result.intent() == QuestionIntent.DASHBOARD_ANALYSIS || result.intent() == QuestionIntent.DATABASE_STATISTICS);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "when was the recall issued?",
            "what is the severity of the recall?",
            "what was the reason for the recall?",
            "details of the recall"
    })
    void testClassifyUnspecifiedRecallDetailPromptsClarification(String question) {
        QuestionClassification result = classifier.classify(question);
        assertEquals(QuestionIntent.RECALL_DETAILS, result.intent());
        assertNotNull(result.clarificationPrompt());
        assertTrue(result.clarificationPrompt().contains("Which recall are you asking about?"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "how can i check if my product is recalled?",
            "what should i check before buying a used crib?",
            "product safety advice",
            "safety tips for household electronics"
    })
    void testClassifyGeneralSafetyAdvice(String question) {
        QuestionClassification result = classifier.classify(question);
        assertTrue(result.intent() == QuestionIntent.GENERAL_SAFETY || result.intent() == QuestionIntent.GENERAL_PRODUCT_SAFETY || result.intent() == QuestionIntent.GENERAL_SAFETY_ADVICE);
    }

    @Test
    void testClassifyProductRecallCheckWithModelNumber() {
        QuestionClassification result = classifier.classify("Is model FP-992 recalled?");
        assertTrue(result.intent() == QuestionIntent.PRODUCT_RECALL_CHECK || result.intent() == QuestionIntent.SPECIFIC_RECALL_LOOKUP);
        assertEquals("FP-992", result.modelNumber());
        assertTrue(result.hasSpecificIdentifier());
    }

    @Test
    void testClassifyBatchCheckWithBatchNumber() {
        QuestionClassification result = classifier.classify("Check batch B-440");
        assertTrue(result.intent() == QuestionIntent.BATCH_INVESTIGATION || result.intent() == QuestionIntent.BATCH_CHECK || result.intent() == QuestionIntent.PRODUCT_RECALL_CHECK || result.intent() == QuestionIntent.SPECIFIC_RECALL_LOOKUP);
        assertEquals("B-440", result.batchNumber());
        assertTrue(result.hasSpecificIdentifier());
    }

    @Test
    void testClassifyReportAssistance() {
        QuestionClassification result = classifier.classify("How can I export a compliance report in PDF?");
        assertEquals(QuestionIntent.REPORT_ASSISTANCE, result.intent());
    }

    @Test
    void testClassifyRecallDetailsWithRecallId() {
        QuestionClassification result = classifier.classify("Explain recall #16");
        assertEquals(QuestionIntent.RECALL_DETAILS, result.intent());
        assertEquals(16L, result.recallId());
        assertTrue(result.hasSpecificIdentifier());
    }

    @Test
    void testClassifyProductComparison() {
        QuestionClassification result = classifier.classify("Compare Samsung and Oppo recall records");
        assertEquals(QuestionIntent.PRODUCT_COMPARISON, result.intent());
    }

    @Test
    void testClassifyRiskAnalysis() {
        QuestionClassification result = classifier.classify("Which products have the highest recall risk?");
        assertEquals(QuestionIntent.RISK_ANALYSIS, result.intent());
    }
}
