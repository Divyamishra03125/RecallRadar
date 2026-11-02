package com.recallradar.recall.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecallSeverityAuditRepository extends JpaRepository<RecallSeverityAudit, Long> {

    List<RecallSeverityAudit> findByRecallIdOrderByCreatedAtDesc(Long recallId);
}
