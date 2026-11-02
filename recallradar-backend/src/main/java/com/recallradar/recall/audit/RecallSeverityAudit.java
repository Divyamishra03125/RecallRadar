package com.recallradar.recall.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "recall_severity_audit")
@Getter
@Setter
@NoArgsConstructor
public class RecallSeverityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recall_id", nullable = false)
    private Long recallId;

    @Column(name = "old_severity", nullable = false, length = 30)
    private String oldSeverity;

    @Column(name = "new_severity", nullable = false, length = 30)
    private String newSeverity;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "classification_source", length = 150)
    private String classificationSource;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public RecallSeverityAudit(Long recallId, String oldSeverity, String newSeverity, String changedBy, String reason) {
        this(recallId, oldSeverity, newSeverity, changedBy, reason, null, null);
    }

    public RecallSeverityAudit(Long recallId, String oldSeverity, String newSeverity, String changedBy, String reason, String evidence, String classificationSource) {
        this.recallId = recallId;
        this.oldSeverity = oldSeverity;
        this.newSeverity = newSeverity;
        this.changedBy = changedBy;
        this.reason = reason;
        this.evidence = evidence;
        this.classificationSource = classificationSource;
        this.createdAt = OffsetDateTime.now();
    }
}
