package com.recallradar.ai;

import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecallResponseFormatterTest {

    @Test
    void testFormatSeverityBadge() {
        assertEquals("🟠 HIGH", RecallResponseFormatter.formatSeverityBadge("HIGH"));
        assertEquals("🔴 CRITICAL", RecallResponseFormatter.formatSeverityBadge("critical"));
        assertEquals("🟡 MEDIUM", RecallResponseFormatter.formatSeverityBadge("MEDIUM"));
        assertEquals("🟡 MEDIUM", RecallResponseFormatter.formatSeverityBadge("moderate"));
        assertEquals("🔵 LOW", RecallResponseFormatter.formatSeverityBadge("LOW"));
        assertEquals("⚪ UNKNOWN", RecallResponseFormatter.formatSeverityBadge(null));
        assertEquals("⚪ UNKNOWN", RecallResponseFormatter.formatSeverityBadge("   "));
        assertEquals("⚪ UNKNOWN_SEV", RecallResponseFormatter.formatSeverityBadge("UNKNOWN_SEV"));
    }

    @Test
    void testFormatStatusBadge() {
        assertEquals("🟢 ACTIVE", RecallResponseFormatter.formatStatusBadge("ACTIVE"));
        assertEquals("🟢 ACTIVE", RecallResponseFormatter.formatStatusBadge("active"));
        assertEquals("⚪ INACTIVE", RecallResponseFormatter.formatStatusBadge("INACTIVE"));
        assertEquals("⚪ INACTIVE", RecallResponseFormatter.formatStatusBadge("CLOSED"));
        assertEquals("⚪ INACTIVE", RecallResponseFormatter.formatStatusBadge("RESOLVED"));
        assertEquals("⚪ Unknown", RecallResponseFormatter.formatStatusBadge(null));
        assertEquals("⚪ Unknown", RecallResponseFormatter.formatStatusBadge(""));
        assertEquals("⚪ PENDING", RecallResponseFormatter.formatStatusBadge("PENDING"));
    }

    @Test
    void testFormatRecallCardFullDetails() {
        Product product = new Product();
        product.setName("Cordless Vacuum");
        product.setBrand("CleanAir");
        product.setCategory("Appliances");
        product.setModelNumber("CV-900");
        product.setBatchNumber("BATCH-2025");

        Recall recall = new Recall();
        recall.setId(789L);
        recall.setTitle("Overheating motor hazard");
        recall.setStatus("ACTIVE");
        recall.setSeverity("HIGH");
        recall.setRecallDate(LocalDate.of(2025, 4, 15));
        recall.setReason("Internal motor can overheat and cause a fire.");
        recall.setSourceUrl("https://example.com/recall/789");
        recall.setProduct(product);

        String card = RecallResponseFormatter.formatRecallCard(recall);

        assertTrue(card.contains("### ⚠️ Recall ID #789: Overheating motor hazard"));
        assertTrue(card.contains("- **Product:** Cordless Vacuum"));
        assertTrue(card.contains("- **Brand:** CleanAir"));
        assertTrue(card.contains("- **Category:** Appliances"));
        assertTrue(card.contains("- **Model Number:** `CV-900`"));
        assertTrue(card.contains("- **Batch / Lot Number:** `BATCH-2025`"));
        assertTrue(card.contains("🟢 ACTIVE"));
        assertTrue(card.contains("🟠 HIGH"));
        assertTrue(card.contains("- **Recall Date:** 2025-04-15"));
        assertTrue(card.contains("**Hazard & Risk:**\nInternal motor can overheat and cause a fire."));
        assertTrue(card.contains("[View Official Recall Notice](https://example.com/recall/789)"));
    }

    @Test
    void testFormatRecallCardHandlesNullsGracefully() {
        assertEquals("", RecallResponseFormatter.formatRecallCard(null));

        Recall recall = new Recall();
        recall.setId(10L);
        recall.setStatus(null);
        String card = RecallResponseFormatter.formatRecallCard(recall);

        assertTrue(card.contains("### ⚠️ Recall ID #10"));
        assertTrue(card.contains("Unknown"));
        assertTrue(card.contains("UNKNOWN"));
        assertTrue(card.contains("Date not available"));
        assertTrue(card.contains("Source unavailable"));

        // When default recall is used without clearing status
        Recall defaultRecall = new Recall();
        defaultRecall.setId(11L);
        String defaultCard = RecallResponseFormatter.formatRecallCard(defaultRecall);
        assertTrue(defaultCard.contains("ACTIVE"));
    }

    @Test
    void testFormatCasesAThroughE() {
        // Case A
        String caseA = RecallResponseFormatter.formatCaseAProductNotFound("Nonexistent Phone");
        assertTrue(caseA.contains("Product Not Found in Catalogue"));
        assertTrue(caseA.contains("Nonexistent Phone"));

        // Case B
        Product p = new Product();
        p.setId(1L);
        p.setName("Safe Product");
        String caseB = RecallResponseFormatter.formatCaseBProductNoRecalls(p);
        assertTrue(caseB.contains("Safe Product"));
        assertTrue(caseB.contains("No recall notices are currently associated"));

        // Case D
        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("Safe Product Ultra");
        String caseD = RecallResponseFormatter.formatCaseDAmbiguousProducts("Safe Product", List.of(p, p2));
        assertTrue(caseD.contains("Multiple Matching Products Found"));
        assertTrue(caseD.contains("Safe Product Ultra"));

        // Case E
        String caseE = RecallResponseFormatter.formatCaseERecallNotFound(999L);
        assertTrue(caseE.contains("Recall ID #999 does not exist"));
    }

    @Test
    void testFormatDegradedResponseEmptyAndPopulated() {
        assertEquals("No matching recall records were found in the RecallRadar database.",
                RecallResponseFormatter.formatDegradedResponse(null));
        assertEquals("No matching recall records were found in the RecallRadar database.",
                RecallResponseFormatter.formatDegradedResponse(List.of()));

        Recall r1 = new Recall();
        r1.setId(1L);
        r1.setTitle("Test Recall 1");

        Recall r2 = new Recall();
        r2.setId(2L);
        r2.setTitle("Test Recall 2");

        String formatted = RecallResponseFormatter.formatDegradedResponse(List.of(r1, r2));
        assertTrue(formatted.contains("temporarily unavailable"));
        assertTrue(formatted.contains("Recall ID #1"));
        assertTrue(formatted.contains("Recall ID #2"));
        assertTrue(formatted.contains("---"));
        assertTrue(formatted.contains("SaferProducts.gov"));
    }

    @Test
    void testFormatDatabaseStatistics() {
        String stats = RecallResponseFormatter.formatDatabaseStatistics(12L, 4L, null);

        assertTrue(stats.contains("📊 RecallRadar Database Statistics"));
        assertTrue(stats.contains("🟢 **Active Recalls** | **12**"));
        assertTrue(stats.contains("⚪ **Inactive Recalls** | **4**"));
        assertTrue(stats.contains("📦 **Total Recorded** | **16**"));
        assertTrue(stats.contains("• Active Recalls: 12"));
        assertTrue(stats.contains("• Inactive Recalls: 4"));
        assertTrue(stats.contains("• Total Recalls Recorded: 16"));

        String inactiveZero = RecallResponseFormatter.formatDatabaseStatistics(10L, 0L, "INACTIVE");
        assertTrue(inactiveZero.contains("No inactive product recalls were found"));

        String inactiveNonZero = RecallResponseFormatter.formatDatabaseStatistics(10L, 3L, "INACTIVE");
        assertTrue(inactiveNonZero.contains("3 inactive recall(s) out of 13 total recall(s)"));
    }
}
