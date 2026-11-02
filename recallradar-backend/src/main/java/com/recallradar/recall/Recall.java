package com.recallradar.recall;

import com.recallradar.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recall")
@Getter
@Setter
@NoArgsConstructor
public class Recall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_record_id", unique = true, length = 150)
    private String sourceRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 30)
    private String severity;

    @Column(name = "severity_evidence", columnDefinition = "TEXT")
    private String severityEvidence;

    @Column(name = "severity_classification_source", length = 150)
    private String severityClassificationSource;

    @Column(name = "recall_date")
    private LocalDate recallDate;

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "association_status", nullable = false, length = 30)
    private String associationStatus = "UNLINKED";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }

        if (status == null) {
            status = "ACTIVE";
        }

        if (associationStatus == null) {
            associationStatus = (product != null) ? "LINKED" : "UNLINKED";
        }
    }
}
