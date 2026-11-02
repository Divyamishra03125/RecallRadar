package com.recallradar.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String role = "VIEWER"; // ADMIN, ANALYST, VIEWER

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "account_status", nullable = false, length = 50)
    private String accountStatus = "ACTIVE"; // ACTIVE, PENDING_APPROVAL, REJECTED, DISABLED

    @Column(length = 150)
    private String organization;

    @Column(length = 150)
    private String department;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (role == null) {
            role = "VIEWER";
        }
        if (accountStatus == null) {
            accountStatus = "ACTIVE";
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public AppUser(String username, String email, String passwordHash, String role, String fullName) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role.toUpperCase() : "VIEWER";
        this.fullName = fullName;
        this.enabled = true;
        this.accountStatus = "ACTIVE";
        this.emailVerified = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
}
