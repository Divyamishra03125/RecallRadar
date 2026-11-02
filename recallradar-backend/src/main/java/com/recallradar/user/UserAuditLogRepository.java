package com.recallradar.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    List<UserAuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    Page<UserAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
