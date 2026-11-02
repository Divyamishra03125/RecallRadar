package com.recallradar.ai.service;

import com.recallradar.ai.QuestionClassification;
import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecallContextBuilder {

    /**
     * Builds a compact, structured prompt context containing only relevant records,
     * explicitly flagging missing fields and stating factual boundaries.
     */
    public String buildStructuredContext(
            String question,
            QuestionClassification classification,
            List<Product> products,
            List<Recall> recalls
    ) {
        return buildStructuredContext(question, classification, products, recalls, 0L);
    }

    public String buildStructuredContext(
            String question,
            QuestionClassification classification,
            List<Product> products,
            List<Recall> recalls,
            long activeRecalls
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== RECALLRADAR GROUNDED DATABASE CONTEXT ===\n");
        sb.append("User Question: ").append(question).append("\n");
        sb.append("Detected Intent: ").append(classification.intent()).append("\n");
        sb.append("Active Surveillance Recalls in Database: ").append(activeRecalls).append("\n\n");

        // 1. Matched Products
        sb.append("--- MATCHED PRODUCT CATALOGUE RECORDS ---\n");
        if (products == null || products.isEmpty()) {
            sb.append("No matching registered products in current catalogue.\n\n");
        } else {
            for (Product p : products) {
                sb.append("• Product ID: ").append(p.getId()).append("\n");
                sb.append("  Name: ").append(p.getName()).append("\n");
                sb.append("  Brand: ").append(StringUtils.hasText(p.getBrand()) ? p.getBrand() : "[Brand: Not specified]").append("\n");
                sb.append("  Category: ").append(StringUtils.hasText(p.getCategory()) ? p.getCategory() : "[Category: Uncategorized]").append("\n");
                sb.append("  Model Number: ").append(StringUtils.hasText(p.getModelNumber()) ? p.getModelNumber() : "[Model: Unavailable in catalogue]").append("\n");
                sb.append("  Batch Number: ").append(StringUtils.hasText(p.getBatchNumber()) ? p.getBatchNumber() : "[Batch: Unavailable in catalogue]").append("\n\n");
            }
        }

        // 2. Matched Recalls
        sb.append("--- MATCHED RECALL NOTICES ---\n");
        List<String> missingFields = new ArrayList<>();

        if (recalls == null || recalls.isEmpty()) {
            sb.append("No matching recall records were found in the RecallRadar database.\n\n");
        } else {
            for (Recall r : recalls) {
                sb.append("• Recall ID: ").append(r.getId()).append("\n");
                sb.append("  Title: ").append(r.getTitle()).append("\n");
                sb.append("  Severity: ").append(r.getSeverity()).append("\n");
                sb.append("  Status: ").append(r.getStatus()).append("\n");
                sb.append("  Association: ").append(r.getAssociationStatus()).append("\n");

                if (r.getRecallDate() != null) {
                    sb.append("  Publication Date: ").append(r.getRecallDate()).append("\n");
                } else {
                    sb.append("  Publication Date: [Date unavailable]\n");
                    missingFields.add("Recall #" + r.getId() + " Publication Date");
                }

                if (StringUtils.hasText(r.getDescription())) {
                    sb.append("  Description: ").append(r.getDescription()).append("\n");
                } else {
                    sb.append("  Description: [No description provided]\n");
                    missingFields.add("Recall #" + r.getId() + " Description");
                }

                if (StringUtils.hasText(r.getReason())) {
                    sb.append("  Hazard / Reason: ").append(r.getReason()).append("\n");
                } else {
                    sb.append("  Hazard / Reason: [No reason recorded]\n");
                }

                if (StringUtils.hasText(r.getSeverityEvidence())) {
                    sb.append("  Severity Evidence: ").append(r.getSeverityEvidence()).append("\n");
                }
                if (StringUtils.hasText(r.getSeverityClassificationSource())) {
                    sb.append("  Classification Source: ").append(r.getSeverityClassificationSource()).append("\n");
                }

                if (StringUtils.hasText(r.getSourceUrl())) {
                    sb.append("  Official Source URL: ").append(r.getSourceUrl()).append("\n");
                } else {
                    sb.append("  Official Source URL: [Source unavailable]\n");
                    missingFields.add("Recall #" + r.getId() + " Source URL");
                }

                Product p = r.getProduct();
                if (p != null) {
                    sb.append("  Associated Product: #").append(p.getId()).append(" - ").append(p.getName()).append("\n");
                    if (StringUtils.hasText(p.getModelNumber())) sb.append("  Associated Model: ").append(p.getModelNumber()).append("\n");
                    if (StringUtils.hasText(p.getBatchNumber())) sb.append("  Associated Batch: ").append(p.getBatchNumber()).append("\n");
                } else {
                    sb.append("  Associated Product: [UNLINKED - Surveillance Docket Notice]\n");
                }
                sb.append("\n");
            }
        }

        // 3. Known Missing Fields
        sb.append("--- KNOWN MISSING INFORMATION ---\n");
        if (missingFields.isEmpty()) {
            sb.append("None explicitly recorded as missing.\n\n");
        } else {
            for (String mf : missingFields) {
                sb.append("- ").append(mf).append("\n");
            }
            sb.append("\n");
        }

        // 4. Strict Behavioral Directives
        sb.append("=== INSTRUCTIONS TO AI MODEL ===\n");
        sb.append("1. Answer ONLY using the facts present in the database context above.\n");
        sb.append("2. NEVER invent recall notices, recall IDs, model numbers, dates, or hazards.\n");
        sb.append("3. If severity is UNKNOWN, preserve it as UNKNOWN; do not upgrade or downgrade.\n");
        sb.append("4. If no recall records match, explicitly state that no recall was found in the database. "
                + "NEVER state or imply that the product is completely safe or defect-free.\n");
        sb.append("5. For each recall, summarize: 1) Overview & Hazard, 2) Stored Severity & Status, 3) Identifiers, 4) Source & Missing data, 5) Recommended consumer verification steps.\n");

        return sb.toString();
    }
}
