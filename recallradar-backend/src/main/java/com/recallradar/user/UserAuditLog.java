package com.recallradar.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_username", nullable = false, length = 100)
    private String targetUsername;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "previous_value", length = 100)
    private String previousValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UserAuditLog(Long targetUserId, String targetUsername, String performedBy,
                        String action, String previousValue, String newValue, String details) {
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.performedBy = performedBy;
        this.action = action;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.details = details;
        this.createdAt = OffsetDateTime.now();
    }
}
