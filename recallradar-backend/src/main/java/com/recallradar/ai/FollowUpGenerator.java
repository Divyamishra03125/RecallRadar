package com.recallradar.ai;

import com.recallradar.recall.Recall;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FollowUpGenerator {

    public List<String> generateFollowUps(
            QuestionClassification classification,
            String question,
            List<Recall> relevantRecalls,
            String answer) {

        Set<String> suggestions = new LinkedHashSet<>();
        QuestionIntent intent = classification != null ? classification.intent() : QuestionIntent.RECALL_SEARCH;

        switch (intent) {
            case GREETING_OR_CHITCHAT -> {
                suggestions.add("How can I check if a product has been recalled?");
                suggestions.add("Are there any critical recalls in the database?");
                suggestions.add("Check if Samsung Galaxy S24 has any recalls");
                suggestions.add("How many products are registered in RecallRadar?");
            }

            case APPLICATION_HELP, HELP_AND_CAPABILITIES -> {
                suggestions.add("Search for Samsung Galaxy S24");
                suggestions.add("Show all active recalls");
                suggestions.add("What does UNKNOWN severity mean?");
            }

            case DASHBOARD_ANALYSIS, DATABASE_STATISTICS -> {
                suggestions.add("Show latest recalls");
                suggestions.add("Which products are currently at risk?");
                suggestions.add("Are there any critical recalls recorded?");
            }

            case SEVERITY_ANALYSIS -> {
                suggestions.add("Explain the difference between HIGH and CRITICAL severity");
                suggestions.add("Which products are associated with high-severity recalls?");
                suggestions.add("Show all active enforcement notices");
            }

            case RISK_ANALYSIS, RISK_EXPLANATION -> {
                suggestions.add("Which products have the highest recall risk?");
                suggestions.add("What should I do if my product has a safety recall?");
                suggestions.add("Check if any battery recalls exist in the database");
            }

            case PRODUCT_COMPARISON -> {
                suggestions.add("Compare Samsung and BrandX recalls");
                suggestions.add("Which brand has higher severity recalls?");
                suggestions.add("Show active recalls by category");
            }

            case GENERAL_SAFETY, GENERAL_PRODUCT_SAFETY, GENERAL_SAFETY_ADVICE -> {
                suggestions.add("How do I find a product's model and batch number?");
                suggestions.add("What should I check before buying a used smartphone?");
                suggestions.add("What official safety portals can I verify with?");
            }

            case PRODUCT_DETAILS, PRODUCT_SEARCH -> {
                if (classification != null && classification.brand() != null) {
                    suggestions.add("Does " + classification.brand() + " have any active recalls?");
                } else {
                    suggestions.add("Does this product have any associated recalls?");
                }
                suggestions.add("Check if model SM-S921B is affected by a recall");
                suggestions.add("Show all registered products in the catalogue");
            }

            case BATCH_INVESTIGATION, BATCH_CHECK -> {
                if (classification != null && classification.batchNumber() != null) {
                    suggestions.add("Check batch " + classification.batchNumber() + " safety details");
                }
                suggestions.add("Does this batch have any linked recalls?");
                suggestions.add("Where do I find the batch number on the packaging?");
            }

            case REPORT_ASSISTANCE -> {
                suggestions.add("How do I export an audit report in PDF?");
                suggestions.add("Can I export recall data in CSV format?");
                suggestions.add("Show active recalls by date range");
            }

            case SPECIFIC_RECALL_LOOKUP, PRODUCT_RECALL_CHECK, RECALL_DETAILS, RECALL_SEARCH, UNKNOWN_OR_UNSUPPORTED, UNKNOWN -> {
                if (relevantRecalls != null && !relevantRecalls.isEmpty()) {
                    if (relevantRecalls.size() == 1) {
                        Recall single = relevantRecalls.get(0);
                        suggestions.add("Tell me more about Recall ID #" + single.getId());
                        if (single.getProduct() != null && StringUtils.hasText(single.getProduct().getBrand())) {
                            suggestions.add("Are there other recalls from " + single.getProduct().getBrand().trim() + "?");
                        }
                        if (StringUtils.hasText(single.getSourceUrl())) {
                            suggestions.add("Where can I find the official manufacturer notice?");
                        }
                        suggestions.add("What remedy or refund is offered for Recall ID #" + single.getId() + "?");
                    } else {
                        Recall first = relevantRecalls.get(0);
                        suggestions.add("Tell me more about Recall ID #" + first.getId());
                        suggestions.add("Which of these recalls has the highest severity?");
                        suggestions.add("Are all of these recalls currently active?");
                    }
                } else {
                    suggestions.add("How do I verify on SaferProducts.gov?");
                    suggestions.add("How do I locate the exact model or batch number?");
                    suggestions.add("Are there any critical recalls in the database?");
                }
            }
        }

        return suggestions.stream().limit(3).toList();
    }
}
