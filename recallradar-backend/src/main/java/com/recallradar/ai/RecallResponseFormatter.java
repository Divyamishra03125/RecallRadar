package com.recallradar.ai;

import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

public class RecallResponseFormatter {

    public static String formatSeverityBadge(String severity) {
        if (!StringUtils.hasText(severity)) {
            return "⚪ UNKNOWN";
        }
        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> "🔴 CRITICAL";
            case "HIGH" -> "🟠 HIGH";
            case "MEDIUM", "MODERATE" -> "🟡 MEDIUM";
            case "LOW" -> "🔵 LOW";
            case "UNKNOWN" -> "⚪ UNKNOWN";
            default -> "⚪ " + severity.trim();
        };
    }

    public static String formatStatusBadge(String status) {
        if (!StringUtils.hasText(status)) {
            return "⚪ Unknown";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "🟢 ACTIVE";
            case "INACTIVE", "CLOSED", "RESOLVED" -> "⚪ INACTIVE";
            default -> "⚪ " + status.trim();
        };
    }

    public static String formatProductCard(Product product) {
        if (product == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("### 📦 Product: ").append(product.getName()).append("\n");
        sb.append("- **Product ID:** #").append(product.getId()).append("\n");
        if (StringUtils.hasText(product.getBrand())) {
            sb.append("- **Brand:** ").append(product.getBrand()).append("\n");
        }
        if (StringUtils.hasText(product.getCategory())) {
            sb.append("- **Category:** ").append(product.getCategory()).append("\n");
        }
        if (StringUtils.hasText(product.getModelNumber())) {
            sb.append("- **Model Number:** `").append(product.getModelNumber()).append("`\n");
        }
        if (StringUtils.hasText(product.getBatchNumber())) {
            sb.append("- **Batch / Lot Number:** `").append(product.getBatchNumber()).append("`\n");
        }
        return sb.toString().trim();
    }

    public static boolean isPlaceholderUrl(String url) {
        if (!StringUtils.hasText(url)) return true;
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.contains("localhost") || lower.equals("#") || lower.equals("unavailable") || lower.equals("n/a") || lower.equals("none");
    }

    public static String extractDomain(String url) {
        if (!StringUtils.hasText(url)) return "unknown";
        try {
            String clean = url.trim();
            if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                clean = "https://" + clean;
            }
            java.net.URI uri = new java.net.URI(clean);
            String host = uri.getHost();
            return host != null ? host.replaceAll("^www\\.", "") : "external source";
        } catch (Exception e) {
            return "external source";
        }
    }

    public static String getSourceVerificationStatus(String url) {
        if (isPlaceholderUrl(url)) {
            return "UNAVAILABLE";
        }
        String lower = url.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("cpsc.gov")
                || lower.contains("fda.gov")
                || lower.contains("nhtsa.gov")
                || lower.contains("europa.eu")
                || lower.contains("recalls.gov")
                || lower.contains("productsafety.gov.au")
                || lower.contains("canada.ca")) {
            return "VERIFIED";
        }
        return "AVAILABLE_UNVERIFIED";
    }

    public static String formatRecallCard(Recall recall) {
        if (recall == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### ⚠️ Recall ID #").append(recall.getId()).append(": ").append(recall.getTitle()).append("\n");

        if (recall.getProduct() != null) {
            Product p = recall.getProduct();
            sb.append("- **Product:** ").append(p.getName()).append("\n");
            if (StringUtils.hasText(p.getBrand())) {
                sb.append("- **Brand:** ").append(p.getBrand()).append("\n");
            }
            if (StringUtils.hasText(p.getCategory())) {
                sb.append("- **Category:** ").append(p.getCategory()).append("\n");
            }
            if (StringUtils.hasText(p.getModelNumber())) {
                sb.append("- **Model Number:** `").append(p.getModelNumber()).append("`\n");
            }
            if (StringUtils.hasText(p.getBatchNumber())) {
                sb.append("- **Batch / Lot Number:** `").append(p.getBatchNumber()).append("`\n");
            }
        }

        sb.append("- **Recall Status:** ").append(formatStatusBadge(recall.getStatus())).append("\n");
        sb.append("- **Severity Level:** ").append(formatSeverityBadge(recall.getSeverity())).append("\n");

        if (recall.getRecallDate() != null) {
            sb.append("- **Recall Date:** ").append(recall.getRecallDate()).append("\n");
        } else {
            sb.append("- **Recall Date:** Date not available\n");
        }

        if (StringUtils.hasText(recall.getReason())) {
            sb.append("\n**Hazard & Risk:**\n").append(recall.getReason().trim()).append("\n");
        } else if (StringUtils.hasText(recall.getDescription())) {
            sb.append("\n**Description:**\n").append(recall.getDescription().trim()).append("\n");
        }

        if (StringUtils.hasText(recall.getSourceUrl()) && !isPlaceholderUrl(recall.getSourceUrl())) {
            sb.append("\n**Official Source:** [View Official Recall Notice](")
              .append(recall.getSourceUrl().trim())
              .append(")\n");
        } else {
            sb.append("\n**Official Source:** Source unavailable\n");
        }

        return sb.toString().trim();
    }

    public static String formatCaseAProductNotFound(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🔍 Product Not Found in Catalogue\n\n");
        sb.append("The product **\"").append(query).append("\"** was not found in the current RecallRadar catalogue.\n\n");
        sb.append("Because this product is not registered in our database, its recall status cannot be determined here.\n\n");
        sb.append("---\n\n");
        sb.append("### 🛡️ Product Safety Advice\n");
        sb.append("- The absence of a product in this database does not mean it has no recalls or is hazard-free.\n");
        sb.append("- We recommend searching by exact model number or batch code, or checking official regulatory databases such as [CPSC.gov](https://www.cpsc.gov) or [SaferProducts.gov](https://www.saferproducts.gov).\n\n");
        sb.append("⚠️ *Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe or free from defects.*");
        return sb.toString();
    }

    public static String formatCaseBProductNoRecalls(Product product) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 📦 Product Found: ").append(product.getName()).append("\n\n");
        sb.append(formatProductCard(product)).append("\n\n");
        sb.append("---\n\n");
        sb.append("🟢 **Recall Status:** No recall notices are currently associated with this product in the RecallRadar database.\n\n");
        sb.append("ℹ️ **Safety Note:** This indicates there are no recall records in our database for this registered product. However, this does not certify that the product is completely safe or that no external or pending notices exist.\n\n");
        sb.append("⚠️ *Safety Advisory: Always verify critical safety updates with the manufacturer or official regulatory safety portals such as SaferProducts.gov or CPSC.gov.*");
        return sb.toString();
    }

    public static String formatCaseDAmbiguousProducts(String query, List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 🔍 Multiple Matching Products Found\n\n");
        sb.append("Your search for **\"").append(query).append("\"** matched **").append(products.size()).append("** products in the RecallRadar catalogue:\n\n");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(i + 1).append(". **").append(p.getName()).append("** (ID: #").append(p.getId()).append(")\n");
            sb.append("   - **Brand:** ").append(StringUtils.hasText(p.getBrand()) ? p.getBrand() : "N/A");
            sb.append(" | **Model:** `").append(StringUtils.hasText(p.getModelNumber()) ? p.getModelNumber() : "N/A").append("`");
            sb.append(" | **Batch:** `").append(StringUtils.hasText(p.getBatchNumber()) ? p.getBatchNumber() : "N/A").append("`\n");
        }
        sb.append("\n👉 **Please specify:** Which exact product would you like to investigate? You can provide the **Product ID**, **exact model number**, or **batch code**.");
        return sb.toString();
    }

    public static String formatCaseERecallNotFound(Long recallId) {
        return "### ⚠️ Recall Not Found\n\n"
             + "Recall ID #" + recallId + " does not exist in the current RecallRadar database.\n\n"
             + "Please verify the Recall ID number or search by product name, brand, or model number.";
    }

    public static String formatDegradedResponse(List<Recall> recalls) {
        if (recalls == null || recalls.isEmpty()) {
            return "No matching recall records were found in the RecallRadar database.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ Notice: The Gemini AI summarizer is temporarily unavailable. ")
          .append("Displaying matching records directly from the RecallRadar database:\n\n");

        for (int i = 0; i < recalls.size(); i++) {
            if (i > 0) {
                sb.append("\n---\n\n");
            }
            sb.append(formatRecallCard(recalls.get(i)));
        }

        sb.append("\n\n---\n*Please verify critical safety details with the manufacturer or official safety portals such as SaferProducts.gov.*");
        return sb.toString().trim();
    }

    public static String formatDatabaseStatistics(
            long totalProducts,
            long totalRecalls,
            long activeRecalls,
            long criticalRecalls,
            long highRecalls,
            long unknownRecalls,
            long productsAtRisk) {

        return """
                ### 📊 RecallRadar Database Summary

                | Platform Metric | Count | Description |
                |---|---|---|
                | 📦 **Registered Products** | **%d** | Total catalogue products registered |
                | 📋 **Total Recalls Recorded** | **%d** | All recall notices recorded in database |
                | 🟢 **Active Surveillance Recalls** | **%d** | Currently active safety enforcement notices |
                | ⚠️ **Distinct Products at Risk** | **%d** | Registered products with active safety notices |
                | 🔴 **Critical Severity Tier** | **%d** | Highest hazard level notices |
                | 🟠 **High Severity Tier** | **%d** | High hazard notices |
                | ⚪ **Unknown Severity Tier** | **%d** | External notices without formal severity grade |

                *Summary grounded in active RecallRadar PostgreSQL records.*
                """.formatted(
                totalProducts, totalRecalls, activeRecalls, productsAtRisk, criticalRecalls, highRecalls, unknownRecalls
        );
    }

    public static String formatDirectProductSearchResponse(
            String query,
            List<Product> products,
            List<Recall> recalls) {

        StringBuilder sb = new StringBuilder();

        if (products == null || products.isEmpty()) {
            sb.append("### 🔍 RecallRadar Database Search Result\n\n");
            sb.append("No registered products or recall records matching **\"").append(query).append("\"** were found in the current RecallRadar database.\n\n");
            sb.append("---\n\n");
            sb.append("### 🛡️ Product Safety Advice\n");
            sb.append("- The absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe, certified safe, or hazard-free.\n");
            sb.append("- We recommend verifying directly with the manufacturer or checking official regulatory safety databases such as [CPSC.gov](https://www.cpsc.gov) or [SaferProducts.gov](https://www.saferproducts.gov).\n\n");
            sb.append("⚠️ *Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe or free from defects. Always verify with official safety portals.*");
            return sb.toString();
        }

        sb.append("### 📦 RecallRadar Database Search Results\n\n");
        sb.append("Found registered product(s) matching **\"").append(query).append("\"** in the catalogue:\n\n");

        for (int i = 0; i < products.size(); i++) {
            if (i > 0) sb.append("\n\n");
            sb.append(formatProductCard(products.get(i)));
        }

        sb.append("\n\n---\n\n");

        if (recalls != null && !recalls.isEmpty()) {
            sb.append("### ⚠️ Associated Safety Recalls\n\n");
            sb.append("Found **").append(recalls.size()).append("** recall notice(s) associated with this query:\n\n");
            for (int i = 0; i < recalls.size(); i++) {
                if (i > 0) sb.append("\n\n---\n\n");
                sb.append(formatRecallCard(recalls.get(i)));
            }
        } else {
            sb.append("🟢 **Recall Status:** No associated recall records were found in the current RecallRadar database for this product.\n\n");
            sb.append("⚠️ *Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe or free from defects. Always verify with the manufacturer or official safety portals such as SaferProducts.gov or CPSC.gov.*");
        }

        return sb.toString();
    }

    public static String formatDirectBatchResponse(
            String batchNumber,
            List<Product> products,
            List<Recall> recalls) {

        StringBuilder sb = new StringBuilder();
        sb.append("### 🔍 Batch Traceability Investigation: `").append(batchNumber).append("`\n\n");

        if (recalls != null && !recalls.isEmpty()) {
            sb.append("⚠️ **Recall Match Found:** Batch `").append(batchNumber)
              .append("` is associated with **").append(recalls.size())
              .append("** active product safety recall(s) in the RecallRadar database:\n\n");
            for (int i = 0; i < recalls.size(); i++) {
                if (i > 0) sb.append("\n\n---\n\n");
                sb.append(formatRecallCard(recalls.get(i)));
            }
        } else if (products != null && !products.isEmpty()) {
            Product p = products.get(0);
            sb.append("Product **").append(p.getName()).append("** with batch code `").append(batchNumber)
              .append("` is registered in the catalogue, but has **no active safety recalls** recorded in the RecallRadar database.\n\n");
            sb.append(formatProductCard(p)).append("\n\n");
            sb.append("⚠️ *Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe. Always verify with official safety portals.*");
        } else {
            sb.append("No registered products or recall records matching batch code `").append(batchNumber)
              .append("` were found in the RecallRadar database.\n\n");
            sb.append("⚠️ *Advisory: Absence of a batch record in the RecallRadar database does not guarantee safety. Always verify with the manufacturer or official safety portals such as SaferProducts.gov or CPSC.gov.*");
        }

        return sb.toString();
    }

    public static String formatDirectRecallSearchResponse(
            String queryOrFilter,
            List<Recall> recalls) {

        if (recalls == null || recalls.isEmpty()) {
            return "### 📋 RecallRadar Database Search Results\n\n"
                 + "No recall records matching **\"" + queryOrFilter + "\"** were found in the RecallRadar database.\n\n"
                 + "*Check the manufacturer and relevant official product-safety authorities for the latest information.*";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("### 📋 RecallRadar Database Search Results\n\n");
        sb.append("Found **").append(recalls.size()).append("** matching recall record(s) in the database:\n\n");

        for (int i = 0; i < recalls.size(); i++) {
            if (i > 0) sb.append("\n\n---\n\n");
            sb.append(formatRecallCard(recalls.get(i)));
        }

        return sb.toString();
    }

    public static String formatDatabaseStatistics(long activeRecalls, long inactiveRecalls, String statusFilter) {
        long total = activeRecalls + inactiveRecalls;
        if ("INACTIVE".equalsIgnoreCase(statusFilter)) {
            if (inactiveRecalls == 0) {
                return "📊 RecallRadar Database Statistics: No inactive product recalls were found.";
            }
            return String.format("📊 RecallRadar Database Statistics: %d inactive recall(s) out of %d total recall(s).",
                    inactiveRecalls, total);
        }
        return String.format("""
                ### 📊 RecallRadar Database Statistics
                | Platform Metric | Count |
                |---|---|
                | 🟢 **Active Recalls** | **%d** |
                | ⚪ **Inactive Recalls** | **%d** |
                | 📦 **Total Recorded** | **%d** |

                • Active Recalls: %d
                • Inactive Recalls: %d
                • Total Recalls Recorded: %d
                """, activeRecalls, inactiveRecalls, total, activeRecalls, inactiveRecalls, total);
    }
}
