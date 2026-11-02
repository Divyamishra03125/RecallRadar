package com.recallradar.user.dto;

public record UserStatsResponse(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        long adminCount,
        long analystCount,
        long viewerCount,
        long pendingRequestsCount
) {}
