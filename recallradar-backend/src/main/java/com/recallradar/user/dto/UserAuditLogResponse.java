package com.recallradar.user.dto;

import com.recallradar.user.UserAuditLog;
import java.time.OffsetDateTime;

public record UserAuditLogResponse(
        Long id,
        Long targetUserId,
        String targetUsername,
        String performedBy,
        String action,
        String previousValue,
        String newValue,
        String details,
        OffsetDateTime createdAt
) {
    public static UserAuditLogResponse fromEntity(UserAuditLog log) {
        return new UserAuditLogResponse(
                log.getId(),
                log.getTargetUserId(),
                log.getTargetUsername(),
                log.getPerformedBy(),
                log.getAction(),
                log.getPreviousValue(),
                log.getNewValue(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
